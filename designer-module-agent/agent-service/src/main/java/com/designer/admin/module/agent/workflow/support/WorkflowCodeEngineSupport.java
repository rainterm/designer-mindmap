package com.designer.admin.module.agent.workflow.support;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.constant.AgentConsts;
import com.designer.admin.module.agent.util.AgentJson;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.noear.solon.core.util.RunUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.isJsonValue;
import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.parseCfg;
import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.str;
/**
 * 工作流代码节点：JS / Python 引擎与条件表达式求值。
 *
 * @author 雨夜终端
 */
@Component
public class WorkflowCodeEngineSupport {

    private static final Logger log = LoggerFactory.getLogger(WorkflowCodeEngineSupport.class);
    private static final int CODE_OUTPUT_LIMIT = 50_000;

    /**
     * GraalPy 内嵌执行：注入 __wf_vars__/__wf_script__。
     * 支持顶层 return，或最后一行表达式作为返回值；也可赋值 result。
     */
    private static final String PYTHON_EVAL = ""
            + "import ast, json\n"
            + "\n"
            + "def _as_str(v):\n"
            + "    if v is None:\n"
            + "        return ''\n"
            + "    if isinstance(v, str):\n"
            + "        return v\n"
            + "    if isinstance(v, (bytes, bytearray)):\n"
            + "        return bytes(v).decode('utf-8', 'replace')\n"
            + "    return str(v)\n"
            + "\n"
            + "def _parse_maybe(v):\n"
            + "    if not isinstance(v, str):\n"
            + "        v = _as_str(v)\n"
            + "    t = v.strip()\n"
            + "    if (t.startswith('{') and t.endswith('}')) or (t.startswith('[') and t.endswith(']')):\n"
            + "        try:\n"
            + "            return json.loads(t)\n"
            + "        except Exception:\n"
            + "            return v\n"
            + "    return v\n"
            + "\n"
            + "def _dump(v):\n"
            + "    if v is None:\n"
            + "        return ''\n"
            + "    if isinstance(v, str):\n"
            + "        return v\n"
            + "    if isinstance(v, bool):\n"
            + "        return 'true' if v else 'false'\n"
            + "    if isinstance(v, (dict, list)):\n"
            + "        return json.dumps(v, ensure_ascii=False)\n"
            + "    return str(v)\n"
            + "\n"
            + "_vars = __wf_vars__ if isinstance(__wf_vars__, dict) else json.loads(_as_str(__wf_vars__))\n"
            + "_script = _as_str(__wf_script__)\n"
            + "g = {'__name__': '__main__'}\n"
            + "for _k, _v in _vars.items():\n"
            + "    g[_k] = _parse_maybe(_v)\n"
            + "if 'output' not in g:\n"
            + "    g['output'] = ''\n"
            + "_raw_out = _vars.get('output', '')\n"
            + "_parsed_out = _parse_maybe(_raw_out)\n"
            + "g['obj'] = _parsed_out if isinstance(_parsed_out, (dict, list)) else None\n"
            + "\n"
            + "_tree = ast.parse(_script, mode='exec')\n"
            + "if _tree.body and isinstance(_tree.body[-1], ast.Expr):\n"
            + "    _tree.body[-1] = ast.Return(value=_tree.body[-1].value)\n"
            + "    ast.fix_missing_locations(_tree)\n"
            + "if not _tree.body:\n"
            + "    _tree.body = [ast.Return(value=ast.Constant(value=None))]\n"
            + "    ast.fix_missing_locations(_tree)\n"
            + "_fn = ast.FunctionDef(\n"
            + "    name='__wf_run',\n"
            + "    args=ast.arguments(posonlyargs=[], args=[], vararg=None, kwonlyargs=[], kw_defaults=[], kwarg=None, defaults=[]),\n"
            + "    body=_tree.body,\n"
            + "    decorator_list=[],\n"
            + "    returns=None,\n"
            + ")\n"
            + "ast.fix_missing_locations(_fn)\n"
            + "_mod = ast.Module(body=[_fn], type_ignores=[])\n"
            + "ast.fix_missing_locations(_mod)\n"
            + "exec(compile(_mod, '<workflow>', 'exec'), g, g)\n"
            + "_val = g['__wf_run']()\n"
            + "if _val is None:\n"
            + "    _val = g.get('result', g.get('output', ''))\n"
            + "_wf_out = _dump(_val)\n";

    private final AtomicReference<ScriptEngine> jsEngine = new AtomicReference<>();
    /** 共享 Engine；Context 按线程缓存，避免跨线程复用与反复创建 */
    private final AtomicReference<Engine> pythonEngine = new AtomicReference<>();
    private final AtomicReference<Source> pythonEvalSource = new AtomicReference<>();
    private final AtomicBoolean codeEnginesWarm = new AtomicBoolean(false);

    /** 启动后异步预热 JS / Python，避免首次跑代码节点卡数秒 */
    @Init
    public void warmCodeEngines() {
        RunUtil.asyncAndTry(() -> {
            long t0 = System.currentTimeMillis();
            try {
                resolveJsEngine();
                Source evalSrc = resolvePythonEvalSource();
                try (Context ctx = openPythonContext()) {
                    ctx.eval(Source.create("python", "1"));
                    ctx.eval(Source.create("python",
                            "import json\n__wf_vars__ = {}\n__wf_script__ = '1'\n"));
                    ctx.eval(evalSrc);
                }
                codeEnginesWarm.set(true);
                log.info("工作流代码引擎预热完成, {}ms", System.currentTimeMillis() - t0);
            } catch (Exception e) {
                log.warn("工作流代码引擎预热失败（首次执行可能较慢）", e);
            }
        });
    }

    public String runCode(String config, Map<String, String> vars) throws Exception {
        Map<String, Object> cfg = parseCfg(config);
        String language = str(cfg.get("language"), "js").toLowerCase(Locale.ROOT);
        String script = str(cfg.get("script"), "");
        if (script.isBlank()) {
            throw new BizException("代码节点未填写脚本");
        }
        if ("py".equals(language) || "python".equals(language)) {
            return runPython(script, vars);
        }
        if (!"js".equals(language) && !"javascript".equals(language)) {
            throw new BizException("暂仅支持 JavaScript（language=js）或 Python（language=python）");
        }
        ScriptEngine engine = resolveJsEngine();
        Bindings bindings = engine.createBindings();
        bindWorkflowJsVars(engine, bindings, vars);
        Object result;
        try {
            result = evalJs(engine, bindings, script);
        } catch (ScriptException e) {
            throw new BizException("代码执行失败: " + e.getMessage(), e);
        }
        if (result == null) {
            Object outBinding = bindings.get(AgentConsts.CTX_OUTPUT);
            return outBinding == null ? "" : String.valueOf(outBinding);
        }
        return String.valueOf(result);
    }

    /** 条件表达式按 JavaScript 求值（JSON 字符串变量会先 parse 成对象）。 */
    public boolean evalJsCondition(String expr, Map<String, String> vars) {
        if (expr == null || expr.isBlank()) {
            return true;
        }
        ScriptEngine engine = resolveJsEngine();
        Bindings bindings = engine.createBindings();
        bindWorkflowJsVars(engine, bindings, vars);
        Object result;
        try {
            result = engine.eval(expr, bindings);
        } catch (ScriptException e) {
            throw new BizException("条件表达式执行失败: " + e.getMessage(), e);
        }
        return jsTruthy(result);
    }

    private String runPython(String script, Map<String, String> vars) {
        ensureCodeEnginesWarm();
        try (Context context = openPythonContext()) {
            Value bindings = context.getBindings("python");
            String varsJson = AgentJson.toJson(vars == null ? Map.of() : vars);
            context.eval(Source.create("python",
                    "import json\n"
                            + "__wf_vars__ = json.loads(" + AgentJson.toJson(varsJson) + ")\n"
                            + "__wf_script__ = " + AgentJson.toJson(script) + "\n"));
            context.eval(resolvePythonEvalSource());
            Value out = bindings.hasMember("_wf_out") ? bindings.getMember("_wf_out") : null;
            String text = (out == null || out.isNull()) ? "" : out.asString();
            if (text.length() > CODE_OUTPUT_LIMIT) {
                return text.substring(0, CODE_OUTPUT_LIMIT);
            }
            return text;
        } catch (PolyglotException e) {
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            throw new BizException("Python 执行失败: " + msg, e);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("Python 执行失败: " + e.getMessage(), e);
        }
    }

    /** 若启动预热未完成，则同步完成一次，避免首请求再卡一轮 */
    private void ensureCodeEnginesWarm() {
        if (codeEnginesWarm.get()) {
            return;
        }
        synchronized (codeEnginesWarm) {
            if (codeEnginesWarm.get()) {
                return;
            }
            resolveJsEngine();
            resolvePythonEvalSource();
            try (Context ctx = openPythonContext()) {
                ctx.eval(Source.create("python", "1"));
            } catch (Exception e) {
                throw new BizException("GraalPy 初始化失败: " + e.getMessage(), e);
            }
            codeEnginesWarm.set(true);
        }
    }


    private Context openPythonContext() {
        try {
            return Context.newBuilder("python")
                    .engine(resolvePythonEngine())
                    .allowHostAccess(HostAccess.ALL)
                    .allowHostClassLookup(s -> false)
                    .build();
        } catch (Exception e) {
            throw new BizException("当前运行环境无法加载 GraalPy，请确认已引入 org.graalvm.polyglot:python: "
                    + e.getMessage(), e);
        }
    }

    private Source resolvePythonEvalSource() {
        Source cached = pythonEvalSource.get();
        if (cached != null) {
            return cached;
        }
        try {
            Source created = Source.newBuilder("python", PYTHON_EVAL, "<workflow-eval>")
                    .cached(true)
                    .build();
            if (pythonEvalSource.compareAndSet(null, created)) {
                return created;
            }
            return pythonEvalSource.get();
        } catch (IOException e) {
            throw new BizException("GraalPy 脚本源初始化失败: " + e.getMessage(), e);
        }
    }

    private Engine resolvePythonEngine() {
        Engine cached = pythonEngine.get();
        if (cached != null) {
            return cached;
        }
        try {
            Engine created = Engine.newBuilder()
                    .option("engine.WarnInterpreterOnly", "false")
                    .build();
            if (pythonEngine.compareAndSet(null, created)) {
                return created;
            }
            created.close();
            return pythonEngine.get();
        } catch (Exception e) {
            throw new BizException("当前运行环境无法加载 GraalPy Engine: " + e.getMessage(), e);
        }
    }

    /** 顶层 return 非法，包一层函数后再执行；无 return 的脚本仍按表达式求值 */
    private static Object evalJs(ScriptEngine engine, Bindings bindings, String script) throws ScriptException {
        try {
            return engine.eval(script, bindings);
        } catch (ScriptException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (!msg.toLowerCase(Locale.ROOT).contains("return")) {
                throw e;
            }
            return engine.eval("(function(){\n" + script + "\n})()", bindings);
        }
    }

    private ScriptEngine resolveJsEngine() {
        ScriptEngine cached = jsEngine.get();
        if (cached != null) {
            return cached;
        }
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
        if (engine == null) {
            engine = manager.getEngineByName("js");
        }
        if (engine == null) {
            engine = manager.getEngineByName("nashorn");
        }
        if (engine == null) {
            throw new BizException("当前运行环境无 JavaScript 引擎，请确认已引入 nashorn-core");
        }
        jsEngine.compareAndSet(null, engine);
        return jsEngine.get();
    }

    private void bindWorkflowJsVars(ScriptEngine engine, Bindings bindings, Map<String, String> vars) {
        String outputStr = "";
        if (vars != null) {
            for (Map.Entry<String, String> e : vars.entrySet()) {
                String val = e.getValue() == null ? "" : e.getValue();
                bindings.put(e.getKey(), val);
                if (AgentConsts.CTX_OUTPUT.equals(e.getKey())) {
                    outputStr = val;
                }
            }
        }
        if (!bindings.containsKey(AgentConsts.CTX_OUTPUT)) {
            bindings.put(AgentConsts.CTX_OUTPUT, outputStr);
        } else {
            outputStr = str(bindings.get(AgentConsts.CTX_OUTPUT), outputStr);
        }
        if (vars != null) {
            for (Map.Entry<String, String> e : vars.entrySet()) {
                putJsonBinding(engine, bindings, e.getKey(), e.getValue());
            }
        }
        bindJsonObj(engine, bindings, outputStr);
    }

    private void putJsonBinding(ScriptEngine engine, Bindings bindings, String key, String value) {
        if (key == null || !isJsonValue(value)) {
            return;
        }
        try {
            bindings.put(key, engine.eval("JSON.parse(" + AgentJson.toJson(value) + ")"));
        } catch (Exception e) {
            log.debug("工作流 JS 变量 JSON 解析失败 key={}", key, e);
        }
    }

    private void bindJsonObj(ScriptEngine engine, Bindings bindings, String output) {
        try {
            if (isJsonValue(output)) {
                bindings.put("obj", engine.eval("JSON.parse(" + AgentJson.toJson(output) + ")"));
            } else {
                bindings.put("obj", null);
            }
        } catch (Exception e) {
            log.debug("工作流 JS obj 绑定失败", e);
            bindings.put("obj", null);
        }
    }

    private static boolean jsTruthy(Object r) {
        if (r == null) {
            return false;
        }
        if (r instanceof Boolean b) {
            return b;
        }
        if (r instanceof Number n) {
            double d = n.doubleValue();
            return d != 0 && !Double.isNaN(d);
        }
        if (r instanceof String s) {
            return !s.isEmpty();
        }
        String s = String.valueOf(r);
        return !"undefined".equals(s) && !"null".equals(s) && !"false".equals(s);
    }
}

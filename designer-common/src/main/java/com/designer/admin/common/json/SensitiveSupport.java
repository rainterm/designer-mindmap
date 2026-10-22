package com.designer.admin.common.json;

import com.designer.admin.common.annotation.Sensitive;
import org.noear.eggg.AnnotatedEggg;
import org.noear.eggg.ClassEggg;
import org.noear.eggg.FieldEggg;
import org.noear.eggg.ParamEggg;
import org.noear.eggg.Property;
import org.noear.eggg.PropertyMethodEggg;
import org.noear.snack4.Feature;
import org.noear.snack4.annotation.ONodeAttr;
import org.noear.snack4.annotation.ONodeAttrHolder;
import org.noear.snack4.codec.ObjectCreator;
import org.noear.snack4.codec.ObjectDecoder;
import org.noear.snack4.codec.util.EgggUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 将 {@link Sensitive} 注册为 snack4 属性提炼扩展（等价于 {@code @ONodeAttr(encoder = SensitiveEncoder.class)}）。
 * <p>
 * 必须在任意 snack4 反序列化/序列化之前完成注册（见 {@link com.designer.admin.DesignerApp}），
 * 否则 Eggg 会缓存不含脱敏信息的 ClassEggg。
 *
 * @author 雨夜终端
 */
public final class SensitiveSupport {

    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    static {
        register();
    }

    private SensitiveSupport() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        EgggUtil.addDigestAddin(Sensitive.class, (ce, ae, anno) ->
                toAttrHolder(ce, ae, (Sensitive) anno));
    }

    private static ONodeAttrHolder toAttrHolder(ClassEggg ce, AnnotatedEggg ae, Sensitive anno) {
        return new ONodeAttrHolder(asNodeAttr(anno), resolveName(ae), ae);
    }

    private static String resolveName(AnnotatedEggg s) {
        if (s instanceof FieldEggg) {
            return ((Field) s.getElement()).getName();
        }
        if (s instanceof PropertyMethodEggg) {
            return Property.resolvePropertyName(((Method) s.getElement()).getName());
        }
        if (s instanceof ParamEggg) {
            return ((Parameter) s.getElement()).getName();
        }
        return "";
    }

    /** 构造供 {@link ONodeAttrHolder} 使用的 {@link ONodeAttr} 代理 */
    private static ONodeAttr asNodeAttr(Sensitive sensitive) {
        return (ONodeAttr) Proxy.newProxyInstance(
                ONodeAttr.class.getClassLoader(),
                new Class<?>[]{ONodeAttr.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "encoder" -> SensitiveEncoder.class;
                    case "decoder" -> ObjectDecoder.class;
                    case "creator" -> ObjectCreator.class;
                    case "ignore" -> false;
                    case "encode", "decode", "required" -> true;
                    case "flat" -> false;
                    case "name", "title", "description", "defaultValue", "format", "timezone" -> "";
                    case "features" -> new Feature[0];
                    case "annotationType" -> ONodeAttr.class;
                    case "toString" -> "@ONodeAttr(encoder=SensitiveEncoder.class)";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> method.getDefaultValue();
                });
    }
}

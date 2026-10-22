/**
 * 后台用到的数据字典类型标识。
 *
 * <p><b>为什么集中在这里</b>：字典类型是字符串契约，与后端 {@code sys_dict_type.dict_type}
 * 以及 {@code sql/init/mysql/**} 的种子数据一一对应。散在二十多个页面里写字符串，
 * 拼错一个字符不会报错，只会表现为「下拉为空、列文案退化成码值」，很难排查。</p>
 *
 * <p><b>文案从哪来</b>：一律在后台「系统管理 → 数据字典」维护，前端只认码值。
 * 新增一种状态 / 类型 = 加一条字典项，页面代码不用改。</p>
 */

/** 通用状态：正常 / 停用（系统模块的 status 字段） */
export const DICT_NORMAL_DISABLE = 'sys_normal_disable'

/** 启用状态：启用 / 停用（存储、消息等业务配置的 status 字段） */
export const DICT_ENABLE_DISABLE = 'sys_enable_disable'

/** 执行结果：成功 / 失败（操作日志、登录日志、任务日志、消息记录） */
export const DICT_OPER_RESULT = 'sys_oper_result'

/** 定时任务状态：正常 / 暂停 */
export const DICT_JOB_STATUS = 'sys_job_status'

/** 定时任务失火策略：默认策略 / 立即执行 / 放弃执行 */
export const DICT_JOB_MISFIRE_POLICY = 'sys_job_misfire_policy'

/** 角色数据权限档位：全部数据 / 自定义部门 / 本部门 / 本部门及以下 / 仅本人 */
export const DICT_DATA_SCOPE = 'sys_data_scope'

/** 菜单类型：目录 / 菜单 / 按钮 */
export const DICT_MENU_TYPE = 'sys_menu_type'

/** 访问类型：公开 / 私有 */
export const DICT_STORAGE_ACCESS = 'sys_storage_access'

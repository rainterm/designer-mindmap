/**
 * 菜单红点注册表。
 *
 * <p>每个模块把自己的红点定义（path / load / perm / interval）放在模块内的
 * {@code menuBadges.js}，这里汇总成一份，供 {@code useMenuBadgeStore} 统一拉取与轮询。
 * 新增模块只需要在这里追加一行，菜单渲染逻辑无需改动。</p>
 *
 * <p><b>为什么不把定义直接写在这里</b>：定义要 import 各模块的接口，
 * 放在模块内可以把「菜单红点」和它依赖的接口放在一起，模块删除时不会留下死引用。</p>
 */
export const MENU_BADGES = []

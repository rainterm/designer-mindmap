/**
 * 查询区按钮：仅当换行时靠右对齐
 * 通过对比 .search-actions 与首个条件项的 offsetTop 判断是否换行
 */
export function setupSearchActionsAlign(root = document.body) {
  const sync = () => {
    root.querySelectorAll('.n-form.n-form--inline .n-form-item.search-actions').forEach((el) => {
      const form = el.parentElement
      if (!form) return
      const first = form.querySelector(':scope > .n-form-item:not(.search-actions)')
      if (!first) {
        el.classList.remove('is-wrapped')
        return
      }
      const wrapped = el.offsetTop > first.offsetTop + 2
      el.classList.toggle('is-wrapped', wrapped)
    })
  }

  const resizeObserver = new ResizeObserver(() => sync())
  const observeForms = () => {
    root.querySelectorAll('.n-form.n-form--inline').forEach((form) => {
      resizeObserver.observe(form)
    })
    sync()
  }

  const mutationObserver = new MutationObserver(() => observeForms())
  mutationObserver.observe(root, { childList: true, subtree: true })
  window.addEventListener('resize', sync)
  observeForms()

  return () => {
    resizeObserver.disconnect()
    mutationObserver.disconnect()
    window.removeEventListener('resize', sync)
  }
}

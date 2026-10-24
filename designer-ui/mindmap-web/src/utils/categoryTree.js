/** 树 -> 扁平节点（带 depth / pathLabel） */
export function flattenCategories(nodes = [], depth = 0, parentPath = []) {
  const out = []
  for (const node of nodes || []) {
    const path = [...parentPath, node.name]
    out.push({
      ...node,
      depth,
      pathLabel: path.join(' / '),
      children: node.children || []
    })
    if (node.children?.length) {
      out.push(...flattenCategories(node.children, depth + 1, path))
    }
  }
  return out
}

/** 下拉选项：缩进或路径文案 */
export function toSelectOptions(nodes = [], { usePath = true, excludeId = null } = {}) {
  return flattenCategories(nodes)
    .filter((n) => n.id !== excludeId)
    .filter((n) => {
      if (excludeId == null) return true
      // 排除自身及子孙（移动父级时）
      return !isDescendantOrSelf(nodes, excludeId, n.id)
    })
    .map((n) => ({
      label: usePath ? n.pathLabel : `${'　'.repeat(n.depth)}${n.name}`,
      value: n.id
    }))
}

function isDescendantOrSelf(nodes, ancestorId, id) {
  if (ancestorId === id) return true
  const flat = flattenCategories(nodes)
  const map = new Map(flat.map((n) => [n.id, n]))
  let cur = map.get(id)
  const seen = new Set()
  while (cur && !seen.has(cur.id)) {
    if (cur.id === ancestorId) return true
    seen.add(cur.id)
    cur = map.get(cur.parentId)
  }
  return false
}

export function findCategory(nodes, id) {
  for (const n of nodes || []) {
    if (n.id === id) return n
    const found = findCategory(n.children, id)
    if (found) return found
  }
  return null
}

export function sumTotalCount(nodes = []) {
  return (nodes || []).reduce((sum, n) => sum + (n.totalCount || n.projectCount || 0), 0)
}

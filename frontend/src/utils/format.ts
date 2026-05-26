/**
 * 时间/数字格式化工具
 * 统一全站时间显示：小时(h)，整数不带小数点，小数保留1位
 */

/** 格式化小时数：1 → "1h"，1.5 → "1.5h"，0.5 → "0.5h" */
export function fmtHours(val: number | null | undefined): string {
  if (val == null || val === 0) return '0h'
  if (Number.isInteger(val)) return `${val}h`
  return `${parseFloat(val.toFixed(1))}h`
}

/** 格式化小时数（纯数字，不带h后缀）：1 → "1"，1.5 → "1.5" */
export function fmtHoursNum(val: number | null | undefined): string {
  if (val == null || val === 0) return '0'
  if (Number.isInteger(val)) return `${val}`
  return `${parseFloat(val.toFixed(1))}`
}

/** 格式化百分比：保留0位小数 */
export function fmtPct(val: number | null | undefined): string {
  if (val == null) return '0%'
  return `${Math.round(val)}%`
}

/** 格式化偏差值：正数加+号 */
export function fmtVariance(val: number | null | undefined): string {
  if (val == null || val === 0) return '0h'
  const prefix = val > 0 ? '+' : ''
  if (Number.isInteger(val)) return `${prefix}${val}h`
  return `${prefix}${parseFloat(val.toFixed(1))}h`
}

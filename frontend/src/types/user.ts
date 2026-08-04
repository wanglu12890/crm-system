// 用户相关类型定义文件，包含用户状态、用户信息、用户搜索条件、用户表单数据和用户对话框模式等类型定义。
export type UserStatus = '正常' | '停用'

// TypeScript 接口定义用户对象的结构，包括 id、用户名、姓名、角色、电话、状态和创建时间等属性。
export interface User {
  id: number
  username: string
  name: string
  role: string
  phone: string
  status: UserStatus
  createTime: string
}
// TypeScript 接口定义用户搜索条件的结构，包括用户名、角色和状态等属性。
export interface UserSearchCriteria {
  username: string
  role: string
  status: '' | UserStatus
}
// TypeScript 接口定义用户表单数据的结构，包括 id、用户名、姓名、密码、角色、电话和状态等属性。
export interface UserFormData {
  id?: number
  username: string
  name: string
  password: string
  role: string
  phone: string
  status: UserStatus
}

export type UserDialogMode = 'create' | 'edit'

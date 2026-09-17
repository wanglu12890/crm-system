// export 的作用？export 用于将类型从模块中导出，使其可以在其他文件中被导入和使用。

// 定义 UserStatus 的类型，它可以是字符串 '正常' 或 '停用'，用于表示用户的状态。
export type UserStatus = '正常' | '停用'

// 定义 User 的接口，它描述了用户对象的结构，包括 id、username、name、role、phone、status 和 createTime 等属性。
export interface User {
  id: string
  username: string
  name: string
  roleIds: string[]
  phone: string
  status: UserStatus
  createTime: string
}
// 定义 UserSearchCriteria 的接口，它描述了用户搜索条件的结构，包括 username、role 和 status 等属性。status 属性可以是空字符串或 UserStatus 类型。
export interface UserSearchCriteria {
  username: string
  role: string
  status: '' | UserStatus
}
// 定义 UserFormData 的接口，它描述了用户表单数据的结构，包括 id、username、name、password、role、phone 和 status 等属性。id 属性是可选的，status 属性是 UserStatus 类型。
export interface UserFormData {
  id?: string
  username: string
  name: string
  password: string
  roleIds: string[]
  phone: string
  status: UserStatus
}
export interface CreateUserRequest {
  username: string
  realName: string
  password: string
  phone?: string
  status: 0 | 1
  roleIds: string[]
}

// 定义 UserDialogMode 的类型，它可以是字符串 'create' 或 'edit'，用于表示用户对话框的模式。
export type UserDialogMode = 'create' | 'edit'

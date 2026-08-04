// 使用TypeScript的声明文件来告诉编译器如何处理.vue文件
// 因为TypeScript默认不认识.vue文件，所以我们需要声明一个模块来处理.vue文件的导入
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
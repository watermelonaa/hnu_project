/**
 * @fileoverview ESLint configuration for Vue 3 + TypeScript
 * Follows Google Style Guide, with necessary overrides for Vue/TS.
 * * Google 规范要求：4空格缩进，必须使用分号。
 * Vue/TS 社区惯例：2空格缩进，可选分号。
 * * 此配置在继承 Google 规范的基础上，强制使用 2空格缩进和不使用分号，
 * 以便更好地适应现代前端开发环境。
 */
module.exports = {
  // 指定环境
  'env': {
    'browser': true,
    'es2021': true,
    'node': true
  },
  
  // 指定解析器
  'parser': 'vue-eslint-parser',
  'parserOptions': {
    // 解析 <script> 块中的代码
    'parser': '@typescript-eslint/parser', 
    'ecmaVersion': 'latest',
    'sourceType': 'module',
    'project': './tsconfig.json', // 引用您的 TypeScript 配置文件
    'extraFileExtensions': ['.vue']
  },
  
  // 继承配置：这是核心！
  'extends': [
    // 1. Vue 3 推荐规则
    'plugin:vue/vue3-recommended', 
    // 2. Google 编码规范 (必须在 Vue 规则之后，以便覆盖 Vue 的默认规则)
    'google', 
    // 3. TypeScript 推荐规则
    'plugin:@typescript-eslint/recommended', 
    // 4. Prettier (如果使用 Prettier 来处理格式化，放在最后以避免冲突)
    'prettier' 
  ],
  
  // 规则覆盖和自定义
  'rules': {
    // ------------------------------------------------
    // 强制覆盖 Google 规范的规则 (以适应 Vue/TS 社区惯例)
    // ------------------------------------------------
    
    // 1. 缩进：Google 默认 4 空格，这里强制使用 2 空格
    // Vue 模板的缩进也需要通过 'plugin:vue/vue3-recommended' 进行配置
    'indent': ['error', 2, {
      'SwitchCase': 1,
      // 允许 Vue 模板中的表达式换行
      'FunctionExpression': { 'parameters': 'first' },
      'CallExpression': { 'arguments': 'first' },
      'ArrayExpression': 'first',
      'ObjectExpression': 'first'
    }],
    
    // 2. 分号：Google 默认必须有分号，这里强制不使用分号 (现代JS风格)
    'semi': ['error', 'never'],
    
    // 3. 引号：Google 默认单引号，这里保持单引号
    'quotes': ['error', 'single'],

    // 4. 最大行长：Google 默认 100，这里放宽到 120 (以适应现代宽屏)
    'max-len': ['error', {
      'code': 120,
      'ignoreUrls': true,
      'ignoreStrings': true,
      'ignoreTemplateLiterals': true
    }],
    
    // ------------------------------------------------
    // 针对 Vue 组件和 TypeScript 的规则
    // ------------------------------------------------
    
    // 禁用 TypeScript 的 `no-var-requires`，在某些 node 环境中可能需要 require
    '@typescript-eslint/no-var-requires': 'off',
    
    // 允许使用 any，但在迁移阶段有用 (完成后建议设为 'error')
    '@typescript-eslint/no-explicit-any': 'off', 

    // Vue 组件名必须是 PascalCase (例如: MyComponent)
    'vue/component-name-in-template-casing': ['error', 'PascalCase', {
        'registeredComponentsOnly': false
    }],

    // Vue 属性排序规则
    'vue/attributes-order': 'error',
    'vue/html-self-closing': ['error', {
        'html': {
            'void': 'always',
            'normal': 'always',
            'component': 'always'
        }
    }]
  }
};
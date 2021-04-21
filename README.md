# SQL 注入工具
- 暂只适用于SpringBoot 和Mybatis 和Mysql环境

## 使用
### SQL解析方式注入
1.  SqlParseInject
2.  SqlInject


### 占位符方式注入
1.  PlaceholderInject


### 权限注入控制
1.  SqlInjectOnce.enable(false)
-- 通过ThreadLocal控制，默认使用一次后恢复，可通过SqlInjectOnce.stay(n)控制保留多少次查询
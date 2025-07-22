# 一、MyBatisPlus

## 1. 定义 Mapper

为了简化单表 CRUD，MybatisPlus 提供了一个基础的 BaseMapper<T> 接口，它是 MyBatis-Plus 提供的通用 Mapper 接口，
其中的 T 表示操作的实体类类型（比如 User、Product 等），它已经默认实现了大量常用方法，比如：

| 方法名                                | 说明         |
| ---------------------------------- | ---------- |
| `insert(T entity)`                 | 插入记录       |
| `deleteById(Serializable id)`      | 根据主键删除     |
| `updateById(T entity)`             | 根据主键更新记录   |
| `selectById(Serializable id)`      | 根据主键查询     |
| `selectList(QueryWrapper<T>)`      | 根据条件查询列表   |
| `selectCount(QueryWrapper<T>)`     | 查询总记录数     |
| `selectByMap(Map<String, Object>)` | 根据字段精确匹配查询 |

因此自定义的 Mapper 只要实现了这个 BaseMapper<T> 接口，就无需自己实现单表 CRUD 了。修改 mp-demo 中的 com.itheima.mp.mapper 包下的 UserMapper 接口，
让其继承 BaseMapper：

```java
// 指定泛型为 User，代表要操作的实体类为 User
public interface UserMapper extends BaseMapper<User> {
}
```

测试：

1、测试插入数据，如果使用的是 mybatis 的话，就需要在 Mapper 接口中编写对应的 saveUser 方法，然后在 UserMapper 配置文件中编写 sql 语句，使用 MyBatisPlus 后，
直接调用 BaseMapper 的 insert 方法即可完成插入操作。

```java
@Test
void testInsert() {
    User user = new User();
    user.setId(5L);
    user.setUsername("Lucy");
    user.setPassword("123");
    user.setPhone("18688990011");
    user.setBalance(200);
    user.setInfo("{\"age\": 24, \"intro\": \"英文老师\", \"gender\": \"female\"}");
    user.setCreateTime(LocalDateTime.now());
    user.setUpdateTime(LocalDateTime.now());
    // userMapper.saveUser(user);
    userMapper.insert(user);
}
```

2、测试查询

```java
@Test
void testSelectById() {
    // User user = userMapper.queryUserById(5L);
    User user = userMapper.selectById(5L);
    System.out.println("user = " + user);
}
```

```java
@Test
void testQueryByIds() {
    // List<User> users = userMapper.queryUserByIds(List.of(1L, 2L, 3L, 4L));
    List<User> users = userMapper.selectBatchIds(List.of(1L, 2L, 3L, 4L, 5L));
    users.forEach(System.out::println);
}
```

3、测试修改

```java
@Test
void testUpdateById() {
    User user = new User();
    user.setId(5L);
    user.setBalance(20000);
    // userMapper.updateUser(user);
    userMapper.updateById(user);
}
```

4、测试删除

```java
@Test
void testDeleteUser() {
    // userMapper.deleteUser(5L);
    userMapper.deleteById(5L);
}
```

****
## 2. 常见注解

UserMapper 在继承 BaseMapper 的时候指定了一个泛型，而泛型中的 User 就是与数据库对应的 POJO。MybatisPlus 就是根据 POJO 实体的信息来推断出表的信息，
从而生成 SQL 的。默认情况下：

- MybatisPlus 会把 POJO 实体的类名驼峰转下划线作为表名
- MybatisPlus 会把 POJO 实体的所有变量名驼峰转下划线作为表的字段名，并根据变量类型推断字段类型（可被配置文件修改，但默认是开启驼峰的）
- MybatisPlus 会把名为 id 的字段作为主键

但很多情况下，现实中数据库表/字段的命名经常不符合 Java 类命名规范，例如表名是全大写或没有遵循驼峰转下划线规范、主键字段不是 id 等，所以 MybatisPlus 提供了一些注解便于声明表信息。

1、@TableName：用于指定表名

```java
@TableName("user")
public class User {
    private Long id;
    private String name;
}
```

TableName 注解除了指定表名以外，还可以指定很多其它属性：

| 属性             | 类型    | 必须指定 | 默认值 | 描述                                                         |
| ---------------- | ------- | -------- | ------ | ----------------------------------------------------------- |
| value            | String  | 否       | ""     | 表名                                                         |
| schema           | String  | 否       | ""     | 指定数据库的 schema（模式）                                           |
| keepGlobalPrefix | boolean | 否       | false  | 是否保留全局配置中的表前缀 tablePrefix 的值（当全局 tablePrefix 生效时） |
| resultMap        | String  | 否       | ""     | xml 中 resultMap 的 id（用于满足特定类型的实体类对象绑定）   |
| autoResultMap    | boolean | 否       | false  | 是否自动构建 resultMap 并使用（如果设置 resultMap 则不会进行 resultMap 的自动构建与注入） |
| excludeProperty  | String[]| 否       | {}     | 需要排除的属性名 @since 3.3.1                                |

2、@TableId：用于指定主键及主键策略

```java
@TableName("user")
public class User {
    @TableId
    private Long id;
    private String name;
}
```

| 属性 | 类型 | 必须指定 | 默认值 | 描述 |
| ---- | ---- | ---- | ---- | ---- |
| value | String | 否 | "" | 表名 |
| type | Enum | 否 | IdType.NONE | 指定主键类型 |

IdType 支持的常用类型：

- AUTO：利用数据库的 id 自增长
- INPUT：手动生成 id
- ASSIGN_ID：雪花算法生成 Long 类型的全局唯一 id，这是默认的 ID 策略

3、@TableField：用于字段映射及自动填充控制

一般情况下我们并不需要给字段添加 @TableField 注解，一些特殊情况除外：
 
- 成员变量名与数据库字段名不一致
- 成员变量是以 isXXX 命名，按照 JavaBean 的规范，MybatisPlus 识别字段时会把is去除，这就导致与数据库不符。
- 成员变量名与数据库一致，但是与数据库的关键字冲突。使用 @TableField 注解给字段名添加转义字符：``

****
## 3. 常见配置

MybatisPlus 也支持基于 yaml 文件的自定义配置，大多数的配置都有默认值，因此都无需配置。但还有一些是没有默认值的，例如:

- 实体类的别名扫描包
- 全局 id 类型

```yaml
mybatis-plus:
  type-aliases-package: com.itheima.mp.domain.po
  mapper-locations: "classpath*:/mapper/**/*.xml" # Mapper.xml文件地址，当前这个是默认值
  configuration:
    map-underscore-to-camel-case: true # 是否开启下划线和驼峰的映射
    cache-enabled: false # 是否开启二级缓存
  global-config:
    db-config:
      id-type: auto # 全局id类型为自增长，如果没配置，则默认为雪花算法
      update-strategy: not_null # 更新策略，只更新非空字段
```

需要注意的是，MyBatisPlus 也支持手写 SQL 的，而 mapper 文件的读取地址可以配置，默认值是 classpath*:/mapper/**/*.xml，
也就是说只要把 mapper.xml 文件放置这个目录下就一定会被加载。

****
## 4. 核心功能

### 4.1 条件构造器

除了新增以外，修改、删除、查询的 SQL 语句都需要指定 where 条件，因此 BaseMapper 中提供的相关方法除了以 id 作为 where 条件以外，还支持更加复杂的 where 条件。
MyBatis-Plus 提供了一个非常强大的条件构造器：QueryWrapper / LambdaQueryWrapper 和 UpdateWrapper / LambdaUpdateWrapper，
使用它们可以灵活地构造出复杂的 where 条件。Wrapper 的子类 AbstractWrapper 提供了 where 中包含的所有条件构造方法，
而 QueryWrapper 在 AbstractWrapper 的基础上拓展了一个 select 方法，允许指定查询字段；而 UpdateWrapper 在 AbstractWrapper 的基础上拓展了一个 set 方法，
允许指定 SQL 中的 set 部分。

#### 1. QueryWrapper

QueryWrapper<T> 是 MyBatis-Plus 提供的条件构造器，它主要用于构建查询条件，泛型 <T> 表示实体类类型。QueryWrapper 的核心作用就是 构建查询条件，
然后将这些条件传递给 MyBatis-Plus 提供的 CRUD 方法（如 selectList、selectPage、selectCount 等）来执行具体的数据库操作。
例如：查询查询出名字中带 o 的，存款大于等于 1000 元的人：

```java
@Test
void testQueryWrapper() {
    // 1. 构建查询条件 where name like "%o%" AND balance >= 1000
    QueryWrapper<User> wrapper = new QueryWrapper<User>()
            .select("id", "username", "info", "balance") // 指定查询的字段，等价于 SQL 中的 SELECT 子句
            .like("username", "o") // 模糊查询，等价于 SQL 中的 LIKE
            .ge("balance", 1000); // 添加大于等于条件，等价于 SQL 中的 >=
    // 2. 查询数据
    List<User> users = userMapper.selectList(wrapper); // 作为参数传入
    users.forEach(System.out::println);
}
```

更新用户名为 jack 的用户的余额为 2000：

```java
@Test
void testUpdateByQueryWrapper() {
    // 1. 构建查询条件 where name = "Jack"
    QueryWrapper<User> wrapper = new QueryWrapper<User>()
            .eq("username", "Jack"); // 等价于 SQL 中的 = 
    // 2. 更新数据，user 中非 null 字段都会作为 set 语句
    User user = new User();
    user.setBalance(2000);
    userMapper.update(user, wrapper);
}
```

****
#### 2. UpdateWrapper

基于 BaseMapper 中的 update 方法更新时只能直接赋值，对于一些复杂的需求就难以实现。例如更新 id 为 1,2,4 的用户的余额扣 200，对应的 SQL 应该是：

```sql
UPDATE user SET balance = balance - 200 WHERE id in (1, 2, 4)
```

使用 UpdateWrapper：

```java
@Test
void testUpdateWrapper() {
    List<Long> ids = List.of(1L, 2L, 4L);
    // 1. 生成SQL
    UpdateWrapper<User> wrapper = new UpdateWrapper<User>()
            .setSql("balance = balance - 200") // SET balance = balance - 200
            .in("id", ids); // where id in (1, 2, 4)
    // 2. 更新，注意第一个参数可以给 null，也就是不使用实体类作为更新数据的来源，而是基于 UpdateWrapper 中的 setSQL 来更新内容
    userMapper.update(null, wrapper);
}
```

像之前的 update() 方法，它是依赖创建一个实体对象，然后给需要更新的字段进行赋值，MyBatisPlus 会根据哪些字段不为空进行对应的更新操作，
而这里则不需要再创建对象并赋值字段，直接创建一个条件，然后更新这个数据。

****
#### 3. LambdaQueryWrapper

无论是 QueryWrapper 还是 UpdateWrapper 在构造条件的时候都需要写死字段名称，这在编程规范中显然是不推荐的。通过 方法引用（如 User::getUsername）获取字段的 getter 方法引用，
然后借助反射解析出实际的字段名（如 username），再映射成数据库列名（如 user_name）。所以只要将条件对应的字段的 getter 方法传递给 MybatisPlus，
它就能计算出对应的变量名了，而传递方法可以使用 JDK8 中的方法引用和 Lambda 表达式。因此 MybatisPlus 又提供了一套基于 Lambda 的 Wrapper，包含两个：

- LambdaQueryWrapper
- LambdaUpdateWrapper

```java
@Test
void testLambdaQueryWrapper() {
    // 1. 构建条件 WHERE username LIKE "%o%" AND balance >= 1000
    QueryWrapper<User> wrapper = new QueryWrapper<>();
    wrapper.lambda()
            .select(User::getId, User::getUsername, User::getInfo, User::getBalance)
            .like(User::getUsername, "o")
            .ge(User::getBalance, 1000);
    // 2. 查询
    List<User> users = userMapper.selectList(wrapper);
    users.forEach(System.out::println);
}
```

****














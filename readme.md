# 一、MyBatisPlus

## 1. 定义 Mapper

为了简化单表 CRUD，MybatisPlus 提供了一个基础的 BaseMapper<T> 接口，它是 MyBatis-Plus 提供的通用 Mapper 接口，
其中的 T 表示操作的实体类类型（比如 User、Product 等），它已经默认实现了大量常用方法，比如：

| 方法名                                | 说明         |
|------------------------------------|------------|
| `insert(T entity)`                 | 插入记录       |
| `deleteById(Serializable id)`      | 根据主键删除     |
| `updateById(T entity)`             | 根据主键更新记录   |
| `selectById(Serializable id)`      | 根据主键查询     |
| `selectList(QueryWrapper<T>)`      | 根据条件查询列表   |
| `selectCount(QueryWrapper<T>)`     | 查询总记录数     |
| `selectByMap(Map<String, Object>)` | 根据字段精确匹配查询 |

因此自定义的 Mapper 只要实现了这个 BaseMapper<T> 接口，就无需自己实现单表 CRUD 了。修改 mp-demo 中的 com.itheima.mp.mapper
包下的 UserMapper 接口，
让其继承 BaseMapper：

```java
// 指定泛型为 User，代表要操作的实体类为 User
public interface UserMapper extends BaseMapper<User> {
}
```

测试：

1、测试插入数据，如果使用的是 mybatis 的话，就需要在 Mapper 接口中编写对应的 saveUser 方法，然后在 UserMapper 配置文件中编写
sql 语句，使用 MyBatisPlus 后，
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

UserMapper 在继承 BaseMapper 的时候指定了一个泛型，而泛型中的 User 就是与数据库对应的 POJO。MybatisPlus 就是根据 POJO
实体的信息来推断出表的信息，
从而生成 SQL 的。默认情况下：

- MybatisPlus 会把 POJO 实体的类名驼峰转下划线作为表名
- MybatisPlus 会把 POJO 实体的所有变量名驼峰转下划线作为表的字段名，并根据变量类型推断字段类型（可被配置文件修改，但默认是开启驼峰的）
- MybatisPlus 会把名为 id 的字段作为主键

但很多情况下，现实中数据库表/字段的命名经常不符合 Java 类命名规范，例如表名是全大写或没有遵循驼峰转下划线规范、主键字段不是
id 等，所以 MybatisPlus 提供了一些注解便于声明表信息。

1、@TableName：用于指定表名

```java

@TableName("user")
public class User {
    private Long id;
    private String name;
}
```

TableName 注解除了指定表名以外，还可以指定很多其它属性：

| 属性               | 类型       | 必须指定 | 默认值   | 描述                                                            |
|------------------|----------|------|-------|---------------------------------------------------------------|
| value            | String   | 否    | ""    | 表名                                                            |
| schema           | String   | 否    | ""    | 指定数据库的 schema（模式）                                             |
| keepGlobalPrefix | boolean  | 否    | false | 是否保留全局配置中的表前缀 tablePrefix 的值（当全局 tablePrefix 生效时）             |
| resultMap        | String   | 否    | ""    | xml 中 resultMap 的 id（用于满足特定类型的实体类对象绑定）                        |
| autoResultMap    | boolean  | 否    | false | 是否自动构建 resultMap 并使用（如果设置 resultMap 则不会进行 resultMap 的自动构建与注入） |
| excludeProperty  | String[] | 否    | {}    | 需要排除的属性名 @since 3.3.1                                         |

2、@TableId：用于指定主键及主键策略

```java

@TableName("user")
public class User {
    @TableId
    private Long id;
    private String name;
}
```

| 属性    | 类型     | 必须指定 | 默认值         | 描述     |
|-------|--------|------|-------------|--------|
| value | String | 否    | ""          | 表名     |
| type  | Enum   | 否    | IdType.NONE | 指定主键类型 |

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

除了新增以外，修改、删除、查询的 SQL 语句都需要指定 where 条件，因此 BaseMapper 中提供的相关方法除了以 id 作为 where
条件以外，还支持更加复杂的 where 条件。
MyBatis-Plus 提供了一个非常强大的条件构造器：QueryWrapper / LambdaQueryWrapper 和 UpdateWrapper / LambdaUpdateWrapper，
使用它们可以灵活地构造出复杂的 where 条件。Wrapper 的子类 AbstractWrapper 提供了 where 中包含的所有条件构造方法，
而 QueryWrapper 在 AbstractWrapper 的基础上拓展了一个 select 方法，允许指定查询字段；而 UpdateWrapper 在 AbstractWrapper
的基础上拓展了一个 set 方法，
允许指定 SQL 中的 set 部分。

#### 1. QueryWrapper

QueryWrapper<T> 是 MyBatis-Plus 提供的条件构造器，它主要用于构建查询条件，泛型 <T> 表示实体类类型。QueryWrapper 的核心作用就是
构建查询条件，
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

基于 BaseMapper 中的 update 方法更新时只能直接赋值，对于一些复杂的需求就难以实现。例如更新 id 为 1,2,4 的用户的余额扣
200，对应的 SQL 应该是：

```sql
UPDATE user
SET balance = balance - 200
WHERE id in (1, 2, 4)
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

无论是 QueryWrapper 还是 UpdateWrapper 在构造条件的时候都需要写死字段名称，这在编程规范中显然是不推荐的。通过 方法引用（如
User::getUsername）获取字段的 getter 方法引用，
然后借助反射解析出实际的字段名（如 username），再映射成数据库列名（如 user_name）。所以只要将条件对应的字段的 getter 方法传递给
MybatisPlus，
它就能计算出对应的变量名了，而传递方法可以使用 JDK8 中的方法引用和 Lambda 表达式。因此 MybatisPlus 又提供了一套基于 Lambda
的 Wrapper，包含两个：

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

### 4.2 自定义 SQL

在使用 UpdateWrapper 的时候，写了这样一段代码：

```java
UpdateWrapper<User> wrapper = new UpdateWrapper<User>()
        .setSql("balance = balance - 200") // SET balance = balance - 200
        .in("id", ids);
```

这种写法在某些企业也是不允许的，因为 SQL 语句最好都维护在持久层，而不是业务层。就当前案例来说，由于条件是 in 语句，只能将 SQL
写在 Mapper.xml 文件，
然后利用 foreach 来生成动态 SQL。但这实在是太麻烦了，假如查询条件更复杂，动态 SQL 的编写也会更加复杂。所以，MybatisPlus
提供了自定义 SQL 功能，
可以利用 Wrapper 生成查询条件，再结合 Mapper.xml 编写 SQL。

例如上述代码可以改写成在业务层通过 MyBatisPlus 定义一些复杂的查询条件（where in ...），然后把定义好的条件作为参数传递给
MyBatis 手写的 SQL，

```java

@Test
void testCustomWrapper() {
    // 1. 准备自定义查询条件
    List<Long> ids = List.of(1L, 2L, 4L);
    QueryWrapper<User> wrapper = new QueryWrapper<User>().in("id", ids);
    // 2. 调用 mapper 的自定义方法，直接传递 Wrapper
    userMapper.deductBalanceByIds(200, wrapper);
}
```

然后在 Mapper 层手动拼接条件，需要注意的是，传递的参数必须通过 @Param 设置为 "ew" / Constants.WRAPPER，然后拼接时使用 ${}
拼接字符串的方式

```java

@Select("UPDATE user SET balance = balance - #{money} ${ew.customSqlSegment}")
void deductBalanceByIds(@Param("money") int money, @Param("ew") QueryWrapper<User> wrapper);
```

理论上来讲 MyBatisPlus 是不支持多表查询的，不过可以利用 Wrapper 中自定义条件结合自定义 SQL 来实现多表查询的效果。例如，
查询出所有收货地址在北京的并且用户 id 在 1、2、4 之中的用户，如果使用 MyBatis 是这样的：

```sql
<
select id = "queryUserByIdAndAddr" resultType="com.itheima.mp.domain.User">
SELECT *
FROM user u
         INNER JOIN address a ON u.id = a.user_id
WHERE u.id
    < foreach
    collection="ids" separator="," item="id" open="IN (" close=")"> #{id}
    </foreach
    >
  AND a.city = #{city}
    </
select>
```

但是使用 MyBatisPlus 就可以利用 Wrapper 来构建查询条件，然后手写 SELECT 及 FROM 部分，实现多表查询：

```java

@Test
void testCustomJoinWrapper() {
    // 1. 准备自定义查询条件
    QueryWrapper<User> wrapper = new QueryWrapper<User>()
            .in("u.id", List.of(1L, 2L, 4L))
            .eq("a.city", "北京");
    // 2. 调用 mapper 的自定义方法
    List<User> users = userMapper.queryUserByWrapper(wrapper);
    users.forEach(System.out::println);
}
```

```java

@Select("SELECT u.* FROM user u INNER JOIN address a ON u.id = a.user_id ${ew.customSqlSegment}")
List<User> queryUserByWrapper(@Param(Constants.WRAPPER) QueryWrapper<User> wrapper);
```

也可以在 UserMapper.xml 中写：

```sql
<
select id = "queryUserByIdAndAddr" resultType="com.itheima.mp.domain.User">
SELECT *
FROM user u
         INNER JOIN address a ON u.id = a.user_id ${ew.customSqlSegment}
</
select>
```

****

### 4.3 Service 接口

MybatisPlus 不仅提供了 BaseMapper，还提供了通用的 Service 接口及默认实现，封装了一些常用的 service 模板方法。通用接口为
IService，默认实现为 ServiceImpl，
其中封装的方法可以分为以下几类：

- save：新增
- remove：删除
- update：更新
- get：查询单个结果
- list：查询集合结果
- count：计数
- page：分页查询

#### 1. 基本用法

由于 Service 中经常需要定义与业务有关的自定义方法，所以不能直接使用 IService，而是自定义一个 Service 接口，然后继承
IService 来拓展方法。同时，
让自定义的 Service 的实现类继承 ServiceImpl，这样就不用自己实现 IService 中的接口了。

```java
public interface IUserService extends IService<User> {
}

// 如果后续需要自定义方法，则需要实现 IUserService
public class UserServiceImpl extends ServiceImpl<UserMapper, User> {
}
```

使用 swagger 接口文档，引入相关依赖和 yaml 配置文件：

```xml

<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
    <version>4.4.0</version>
</dependency>

<dependency>
<groupId>org.springdoc</groupId>
<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
<version>2.3.0</version>
</dependency>
```

```yaml
# springdoc-openapi 项目配置
springdoc:
  swagger-ui:
    path: /swagger-ui.html # 指定 Swagger UI 的访问路径，现在设置的是默认路径，它会自动映射导 Knife4j 的默认访问路径 /doc.html
    tags-sorter: alpha # 接口文档页面中，标签（tags）排序方式，alpha 表示按字母顺序排序
    operations-sorter: alpha # 操作（接口方法）排序方式，也用字母序
  api-docs:
    path: /v3/api-docs # 这是 OpenAPI 生成的接口文档 JSON 文件路径，默认 /v3/api-docs，前端页面会根据它请求数据
  group-configs: # 支持多个接口分组配置，方便大型项目拆分不同模块接口文档
    - group: 'default' # 分组名，设置默认分组 default，
      paths-to-match: '/**' # 该分组扫描的接口路径，这里匹配所有接口 /** 
      packages-to-scan: com.itheima.mp.controller # 扫描的包路径，这里只扫描 com.sky 这个包里的 Controller，扫描出来的文档就放到当前设置的分组；如果后面还设置了分组，那么就让每个分组扫描不同的包，达到清晰定位的效果

# knife4j 的增强配置，不需要增强可以不配
knife4j:
  enable: true # 是否启用 Knife4j 增强功能，true 表示启用
  setting:
    language: zh_cn # 界面语言，这里设置为简体中文（zh_cn）
```

使用 IService 接口基于 Restful 风格实现下列接口：

| 编号 | 接口       | 请求方式   | 请求路径                          | 请求参数      | 返回值    |
|----|----------|--------|-------------------------------|-----------|--------|
| 1  | 新增用户     | POST   | /users                        | 用户表单实体    | 无      |
| 2  | 删除用户     | DELETE | /users/{id}                   | 用户id      | 无      |
| 3  | 根据id查询用户 | GET    | /users/{id}                   | 用户id      | 用户VO   |
| 4  | 根据id批量查询 | GET    | /users                        | 用户id集合    | 用户VO集合 |
| 5  | 根据id扣减余额 | PUT    | /users/{id}/deduction/{money} | 用户id、扣减金额 | 无      |

1、新增用户

```java

@PostMapping
@Operation(summary = "新增用户")
public void saveUser(@RequestBody UserFormDTO userFormDTO) {
    // 1. 转换 DTO 为 PO
    User user = BeanUtil.copyProperties(userFormDTO, User.class);
    // 2. 新增
    userService.save(user);
}
```

2、删除用户

```java

@DeleteMapping("/{id}")
@Operation(summary = "删除用户")
public void removeUserById(@PathVariable("id") Long userId) {
    userService.removeById(userId);
}
```

3、根据 id 查询用户

```java

@GetMapping("/{id}")
@Operation(summary = "根据id查询用户")
public UserVO queryUserById(@PathVariable("id") Long userId) {
    // 1. 查询用户
    User user = userService.getById(userId);
    // 2. 处理 vo
    return BeanUtil.copyProperties(user, UserVO.class);
}
```

4、根据 id 批量查询

```java

@GetMapping
@Operation(summary = "根据id集合查询用户")
public List<UserVO> queryUserByIds(@RequestParam("ids") List<Long> ids) {
    List<User> users = userService.listByIds(ids);
    return BeanUtil.copyToList(users, UserVO.class);
}
```

5、根据 id 扣减余额

上述接口都直接在 controller 即可实现，无需编写任何 service 代码，非常方便。但一些带有业务逻辑的接口则需要在 service
中自定义实现，例如此功能的修改操作就涉及：

- 判断用户状态是否正常
- 判断用户余额是否充足

这些业务逻辑都要在 service 层来做，另外更新余额需要自定义 SQL，要在 mapper 中来实现：

Controller 层：

```java

@PutMapping("{id}/deduction/{money}")
@Operation(summary = "扣减用户余额")
public void deductBalance(@PathVariable("id") Long id, @PathVariable("money") Integer money) {
    userService.deductBalance(id, money);
}
```

Service 层：

```java

@Override
public void deductBalance(Long id, Integer money) {
    // 1. 查询用户
    User user = getById(id); // 因为继承了 ServiceImpl<UserMapper, User>，所以可以直接调用父类的方法
    // 2. 判断用户状态
    if (user == null || user.getStatus() == 2) {
        throw new RuntimeException("用户状态异常");
    }
    // 3. 判断用户余额
    if (user.getBalance() < money) {
        throw new RuntimeException("用户余额不足");
    }
    // 4. 扣减余额
    baseMapper.deductMoneyById(id, money);
}
```

Mapper 层：

```java

@Update("UPDATE user SET balance = balance - #{money} WHERE id = #{id}")
void deductMoneyById(@Param("id") Long id, @Param("money") Integer money);
```

****

#### 2. Lambda 查询与修改

实现一个根据复杂条件查询用户的接口，查询条件如下：

- name：用户名关键字，可以为空
- status：用户状态，可以为空
- minBalance：最小余额，可以为空
- maxBalance：最大余额，可以为空

可以理解成一个用户的后台管理界面，管理员可以自己选择条件来筛选用户，因此上述条件不一定存在，需要做判断。

Controller 层：

```java

@GetMapping("/list")
@Operation(summary = "根据id集合查询用户")
public List<UserVO> queryUsers(UserQuery query) {
    // 1. 组织条件
    String username = query.getName();
    Integer status = query.getStatus();
    Integer minBalance = query.getMinBalance();
    Integer maxBalance = query.getMaxBalance();
    LambdaQueryWrapper<User> wrapper = new QueryWrapper<User>().lambda()
            .like(username != null, User::getUsername, username)
            .eq(status != null, User::getStatus, status)
            .ge(minBalance != null, User::getBalance, minBalance)
            .le(maxBalance != null, User::getBalance, maxBalance);
    // 2. 查询用户
    List<User> users = userService.list(wrapper);
    // 3. 处理 vo
    return BeanUtil.copyToList(users, UserVO.class);
}
```

在组织查询条件的时候，加入了 username != null 这样的参数，意思就是当条件成立时才会添加这个查询条件，类似 mapper.xml 文件中的
`<if>` 标签，
这样就实现了动态查询条件效果了。但这种写法仍较为麻烦，所以 Service 中对 LambdaQueryWrapper 和 LambdaUpdateWrapper
的用法进一步做了简化，
无需通过 new 的方式来创建 Wrapper，而是直接调用 lambdaQuery 和 lambdaUpdate 方法：

```java
public List<UserVO> queryUsers(UserQuery query) {
    ...
    List<User> users = userService.lambdaQuery()
            .like(username != null, User::getUsername, username)
            .eq(status != null, User::getStatus, status)
            .ge(minBalance != null, User::getBalance, minBalance)
            .le(maxBalance != null, User::getBalance, maxBalance)
            .list();
}
```

可以发现 lambdaQuery 方法中除了可以构建条件，还需要在链式编程的最后添加一个 list()，这是在告诉 MP 调用结果需要一个 list
集合，其它可选的方法有：

- .one()：最多1个结果
- .list()：返回集合结果
- .count()：返回计数结果

而 IService 中的 lambdaUpdate 同理，例如改造根据 id 修改用户余额的接口，要求如下：

- 如果扣减后余额为 0，则将用户 status 修改为冻结状态（2）

也就是说在扣减用户余额时，需要对用户剩余余额做出判断，如果发现剩余余额为 0，则应该将 status 修改为 2：

```java

@Override
@Transactional
public void deductBalance(Long id, Integer money) {
    ...
    // 扣减余额 update tb_user set balance = balance - ?
    int remainBalance = user.getBalance() - money;
    this.lambdaUpdate()
            .set(User::getBalance, remainBalance) // 更新余额
            .set(remainBalance == 0, User::getStatus, 2) // 动态判断，是否更新 status
            .eq(User::getId, id)
            .eq(User::getBalance, user.getBalance()) // 乐观锁，当前用户的余额与查询到的余额一样时再执行更新
            .update();
}
```

****

#### 3. 批量新增

关于批量插入，如果使用一条一条插入的话，那就是执行 100000 次数据库的插入操作，这样效率肯定是最低的。

```java

@Test
void testSaveOneByOne() {
    long b = System.currentTimeMillis();
    for (int i = 1; i <= 100000; i++) {
        userService.save(buildUser(i));
    }
    long e = System.currentTimeMillis();
    System.out.println("耗时：" + (e - b));
}
```

但如果将数据封装进集合中，每一千条数据发送一次将集合插入数据库的操作，那效率又会高一点。

```java

@Test
void testSaveBatch() {
    List<User> list = new ArrayList<>(1000);
    long b = System.currentTimeMillis();
    for (int i = 1; i <= 100000; i++) {
        list.add(buildUser(i));
        // 每 1000 条批量插入一次
        if (i % 1000 == 0) {
            userService.saveBatch(list);
            list.clear();
        }
    }
    long e = System.currentTimeMillis();
    System.out.println("耗时：" + (e - b));
}
```

但上面 MyBatisPlus 提供的 saveBatch 批量插入操作，在底层仍然是将数据一条一条的插入数据库的，而效率最高的应该是将所有的数据全部封装进一个
SQL 语句，即只执行一次插入操作。
MySQL 的客户端连接参数中有这样的一个参数：rewriteBatchedStatements，它就是重写批处理的 statement 语句。这个参数的默认值是
false，将其配置为 true 即代表开启批处理模式，
开启后可以保证最终只执行 100 次插入。

****

## 5. 扩展功能

### 5.1 代码生成

在使用 MybatisPlus 以后，基础的 Mapper、Service、POJO 代码相对固定，重复编写也比较麻烦，所以 MybatisPlus 官方提供了代码生成器根据数据库表结构生成
POJO、Mapper、Service 等相关代码，
只不过代码生成器同样要编码使用。所以更推荐使用一款 MybatisPlus 的插件，它可以基于图形化界面完成 MybatisPlus 的代码生成。

****

### 5.2 静态工具

有的时候 Service 之间也会相互调用，为了避免出现循环依赖问题，MybatisPlus 提供一个静态工具类 Db，它本质上是一个对 BaseMapper
的静态代理，
内部通过 SpringContextUtils.getBean(Class) 动态获取对应实体类的 BaseMapper<T> 实例，然后再调用其方法，主要是用于简化数据库操作，
可以在没有手动注入 Service 或 Mapper 的前提下执行常用操作。

```java
Db.save(entity);
Db.

updateById(entity);
Db.

removeById(id);
Db.

list(new QueryWrapper<>());
        Db.

getById(id);
// 等价于
userService.

save(entity);
userService.

updateById(entity);
userService.

removeById(id);
userService.

list(queryWrapper);
userService.

getById(id);
```

****

### 5.3 逻辑删除

对于一些比较重要的数据，可以采用逻辑删除的方案，即：

- 在表中添加一个字段标记数据是否被删除
- 当删除数据时把标记置为 true
- 查询时过滤掉标记为 true 的数据

可是一旦采用了逻辑删除，所有的查询和删除逻辑都要跟着变化，非常麻烦。所以 MybatisPlus 就添加了对逻辑删除的支持，
在对应的表和实体类中添加对应的逻辑删除字段（只有MybatisPlus生成的SQL语句才支持自动的逻辑删除，自定义SQL需要自己手动处理逻辑删除），然后在
yaml 中配置逻辑删除字段：

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted # 全局逻辑删除的实体字段名(since 3.3.0,配置后可以忽略不配置步骤2)
      logic-delete-value: 1 # 逻辑已删除值(默认为 1)
      logic-not-delete-value: 0 # 逻辑未删除值(默认为 0)
```

测试：

```java

@Test
void testDeleteByLogic() {
    // 删除方法与以前没有区别
    addressService.removeById(59L);
}
```

实际执行的 SQL 为 update 语句：

```sql
UPDATE address
SET deleted=1
WHERE id = ?
  AND deleted = 0
```

测试查询，发现查询语句多了个 deleted = 0 的条件：

```sql
SELECT id,
       user_id,
       province,
       city,
       town,
       mobile,
       street,
       contact,
       is_default,
       notes,
       deleted
FROM address
WHERE deleted = 0
```

****

### 5.4 通用枚举

在当前的 User 类中有一个 status 字段，它用来表示用户当前的状态，但像这种字段一般应该定义为一个枚举，做业务判断的时候就可以直接基于枚举做比较。
但是目前的数据库采用的是 Integer 类型，对应的 POJO 中的 status 字段也是 Integer 类型，因此业务操作时就必须手动把枚举与
Integer 转换，非常麻烦。
所以 MybatisPlus 提供了一个处理枚举的类型转换器，可以自动完成枚举类型与数据库类型的转换。

定义一个枚举类：

```java

@Getter
public enum UserStatus {
    NORMAL(1, "正常"),
    FREEZE(2, "冻结");
    private final int value;
    private final String desc;

    UserStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
```

修改 User 的 status 字段的类型：

```java
// 使用状态（1正常 2冻结）
private UserStatus status;
```

要让 MybatisPlus 处理枚举与数据库类型自动转换，就必须告诉 MybatisPlus 枚举中的哪个字段的值是作为数据库值的，所以要使用它提供的
@EnumValue 来标记枚举属性：

```java

@EnumValue
private final int value;
```

在 yaml 文件中添加配置枚举处理器：

```java
mybatis-plus:
configuration:
default-enum-type-handler:com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler
```

需要注意的是，需要在 UserStatus 的 desc 字段上添加 @JsonValue 注解，它是用来指定 JSON 序列化时展示的字段，即用 desc
字段表示该枚举本身：

```java

@JsonValue
private final String desc;
```

返回的数据中 status 不再显示 1，而是显示 "正常"：

```json
{
  "id": 1,
  "username": "Jack",
  "info": "{\"age\": 20, \"intro\": \"佛系青年\", \"gender\": \"male\"}",
  "status": "正常",
  "balance": 1600,
  "addresses": null
}
```

****

### 5.5 JSON 处理器

数据库的 user 表中有一个 info 字段，是 JSON 类型：

```json
{
  "age": 20,
  "intro": "佛系青年",
  "gender": "male"
}
```

但目前 User 实体类中却是 String 类型，所以现在读取 info 中的属性时就非常不方便，如果要方便获取，info 的类型最好是一个 Map
或者实体类，可如果把 info 改为对象类型，
在写入数据库时就需要手动转换为 String，读取数据库时又需要手动转换成对象，过程十分繁琐，所以 MybatisPlus
提供了很多特殊类型字段的类型处理器，解决特殊字段类型与数据库类型转换的问题。
例如处理 JSON 就可以使用 JacksonTypeHandler 处理器。

定义一个 [User
Info](./mp-demo/src/main/java/com/itheima/mp/domain/po/UserInfo.java) 实体类来与 info 字段的属性匹配，并修改 User 的 info
字段的类型。
同时在 User 类和对应的字段上上添加一个注解，声明自动映射：

```java

@TableName(value = "user", autoResultMap = true)
public class User {
    @TableField(typeHandler = JacksonTypeHandler.class)
    private UserInfo info;
}
```

```json
{
  "id": 1,
  "username": "Jack",
  "info": {
    "age": 20,
    "intro": "佛系青年",
    "gender": "male"
  },
  "status": "正常",
  "balance": 1600,
  "addresses": null
}
```

****

## 6. 插件功能

### 6.1 分页插件

在未引入分页插件的情况下，MybatisPlus 是不支持分页功能的，IService 和 BaseMapper 中的分页方法都无法正常起效。所以需要配置一个分页配置类：

```java

@Configuration
public class MybatisConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 初始化核心插件
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

在 3.5.9 版本中，MyBatisPlus 对组件做了拆分，比如分页功能依赖的 `jsqlparser` 被单独拆成了 `mybatis-plus-jsqlparser`
包。要想让分页功能跑起来，
需要添加这个依赖，否则 `PaginationInnerInterceptor` 会找不到所需的解析工具：

```xml

<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-jsqlparser</artifactId>
    <version>3.5.9</version> <!-- 确保版本和 MyBatis Plus 主包一致 -->
</dependency>
```

测试：

```java

@Test
void testPageQuery() {
    // 1. 分页查询，new Page() 的两个参数分别是：页码、每页大小
    Page<User> p = userService.page(new Page<>(2, 2));
    // 2. 总条数
    System.out.println("total = " + p.getTotal()); // 4
    // 3. 总页数
    System.out.println("pages = " + p.getPages()); // 2
    // 4. 数据
    List<User> records = p.getRecords();
    records.forEach(System.out::println);
}
```

```sql
SELECT id,
       username,
       password,
       phone,
       info,
       status,
       balance,
       create_time,
       update_time
FROM user LIMIT ?,?
```

****

### 6.2 通用分页实体

实现一个分页查询，前端传递的参数为：

```json
{
  "pageNo": 0,
  "pageSize": 0,
  "sortBy": "",
  "isAsc": true,
  "name": "",
  "status": 0,
  "minBalance": 0,
  "maxBalance": 0
}
```

即可以选择这些作为条件进行分页查询，不设置的话就是使用默认值，pageNo 和 pageSize 等字段在相关的实体类中都有设置默认值，而返回值则需要类似：

```json
{
  "total": 100006,
  "pages": 50003,
  "list": [
    {
      "id": 1685100878975279298,
      "username": "user_9****",
      "info": {
        "age": 24,
        "intro": "英文老师",
        "gender": "female"
      },
      "status": "正常",
      "balance": 2000
    }
  ]
}
```

Controller 层：

```java

@GetMapping("/page")
@Operation(summary = "分页查询")
public PageDTO<UserVO> queryUsersPage(UserQuery query) {
    return userService.queryUsersPage(query);
}
```

Service 层：

```java

@Override
public PageDTO<UserVO> queryUsersPage(UserQuery query) {
    // 1. 构建条件分页条件
    Page<User> page = query.toMpPageDefaultSortByUpdateTimeDesc();
    // 2. 分页查询
    Page<User> records = lambdaQuery()
            .like(query.getName() != null, User::getUsername, query.getName())
            .eq(query.getStatus() != null, User::getStatus, query.getStatus())
            .page(page);
    // 3. 封装为 VO
    // return PageDTO.of(records, UserVO.class);
    // 或者封装自定义的 VO
    // return PageDTO.of(records, user -> BeanUtil.copyProperties(user, UserVO.class));
    return PageDTO.of(records, user -> {
        // 1. 拷贝基础属性
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        // 2. 处理特殊逻辑
        userVO.setUsername(userVO.getUsername().substring(0, userVO.getUsername().length() - 2) + "**");
        return userVO;
    });
}
```

关于分页查询：

```java
Page<User> records = lambdaQuery()
        .like(query.getName() != null, User::getUsername, query.getName())
        .eq(query.getStatus() != null, User::getStatus, query.getStatus())
        .page(page);
```

设置两个条件查询条件，当前端传递的用户名称和用户状态不为空时，则把它们作为分页查询的限制条件，而 .page(page) 里面的那个
page，是构建好的分页条件：

```java

@Data
@Schema(description = "分页查询实体")
public class PageQuery {
    @Schema(description = "页码")
    private Integer pageNo = 1;
    @Schema(description = "每页数据条数")
    private Integer pageSize = 5;
    @Schema(description = "排序字段")
    private String sortBy;
    @Schema(description = "是否升序")
    private Boolean isAsc = true;

    public <T> Page<T> toMpPage(OrderItem... orders) {
        // 1. 分页条件
        Page<T> page = Page.of(pageNo, pageSize);
        // 2. 排序条件，先看前端有没有传排序字段
        if (sortBy != null && !sortBy.isEmpty()) {
            page.addOrder(OrderItem.asc(sortBy));
            return page;
        }
        // 如果前端没有设置排序字段，则根据传递来的排序字段进行排序，
        // 例如：toMpPage(OrderItem.asc("username"), OrderItem.desc("create_time"))
        if (orders != null && orders.length > 0) {
            page.addOrder(orders);
        }
        return page;
    }

    public <T> Page<T> toMpPage(String defaultSortBy, boolean isAsc) {
        if (defaultSortBy != null && !defaultSortBy.isEmpty()) {
            if (!isAsc) {
                return this.toMpPage(OrderItem.desc(defaultSortBy));
            }
        }
        return this.toMpPage(OrderItem.asc(defaultSortBy));
    }

    public <T> Page<T> toMpPageDefaultSortByCreateTimeDesc() {
        return toMpPage("create_time", false);
    }

    public <T> Page<T> toMpPageDefaultSortByUpdateTimeDesc() {
        return toMpPage("update_time", false);
    }
}
```

封装了一个 PageQuery 实体类，专门用来构建分页查询的条件，在这里完成页码以及每页数据条数的初始化，因为 MybatisPlus
提供了查询结果的排序，
所以除了 pageNo 和 pageSize 还设置了排序字段和升降序的标识符。在 Service 层可以自定义排序规则，又因为该类作为前端的接收参数的封装，所以如果前端传递了排序字段，
则按照前端的来，当然这里直接设置为了按照更新时间降序。

它继承了 PageQuery，所以即使 PageQuery 中没有封装 name 和 status 但前端仍能发送这些数据然后被后端接收作为查询条件：

```java

@Data
@Schema(description = "用户查询条件实体")
@EqualsAndHashCode(callSuper = true)
public class UserQuery extends PageQuery {
    @Schema(description = "用户名关键字")
    private String name;
    @Schema(description = "用户状态：1-正常，2-冻结")
    private Integer status;
    @Schema(description = "余额最小值")
    private Integer minBalance;
    @Schema(description = "余额最大值")
    private Integer maxBalance;
}
```

之前有学过，最后的返回结果虽然是一个 List 集合，但集合内部是具体的对象，所以需要返回一个 VO 类型的对象专门展示在前端，所以定义了一个
PageDTO<V> 实体类，
它可以自定义返回的具体类型，所以在 Service 层中可以传入 UserVO 类，将 User 类拷贝给 UserVO：

```java

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页结果")
public class PageDTO<V> {
    @Schema(description = "总条数")
    private Long total;
    @Schema(description = "总页数")
    private Long pages;
    @Schema(description = "返回的数据集合")
    private List<V> list;

    public static <V, P> PageDTO<V> empty(Page<P> p) {
        return new PageDTO<>(p.getTotal(), p.getPages(), Collections.emptyList());
    }

    public static <VO, PO> PageDTO<VO> of(Page<PO> p, Class<VO> voClass) {
        // 1. 非空校验
        List<PO> records = p.getRecords();
        if (records == null || records.isEmpty()) {
            // 无数据，返回空结果
            return empty(p);
        }
        // 2. 数据转换
        List<VO> vos = BeanUtil.copyToList(records, voClass);
        // 3. 封装返回
        return new PageDTO<>(p.getTotal(), p.getPages(), vos);
    }

    public static <VO, PO> PageDTO<VO> of(Page<PO> p, Function<PO, VO> convertor) {
        // 1. 非空校验
        List<PO> records = p.getRecords();
        if (records == null || records.isEmpty()) {
            // 无数据，返回空结果
            return empty(p);
        }
        // 2. 数据转换
        List<VO> vos = records.stream().map(convertor).collect(Collectors.toList());
        // 3. 封装返回
        return new PageDTO<>(p.getTotal(), p.getPages(), vos);
    }
}
```

第二个 of() 方法，允许 Service 层传入一个函数接口，也就是利用 Steam 流来自定义每个 UserVO 中的数据的转换规则：

```java
return PageDTO.of(records, user ->{
// 1. 拷贝基础属性
UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
// 2. 处理特殊逻辑
    userVO.

setUsername(userVO.getUsername().

substring(0,userVO.getUsername().

length() -2)+"**");
        return userVO;
});
```

这里就是将 userName 的最后两个字符变为 **。

****

# 二、Docker

> Docker 的安装参考 Redis 笔记

## 1. 部署 MySQL

传统方式部署 MySQL，大概的步骤有：

- 搜索并下载 MySQL 安装包
- 上传至 Linux 环境
- 编译和配置环境
- 安装

而使用 Docker 安装，只需要一步，在命令行输入下面的命令：

```shell
docker run -d \
  --name mysql \
  -p 3306:3306 \
  -e TZ=Asia/Shanghai \
  -e MYSQL_ROOT_PASSWORD=123 \
  mysql
```

- docker run -d ：创建并运行一个容器，-d 则是让容器以后台进程运行
- --name mysql  : 给容器起个名字叫 mysql
- -p 3306:3306 : 设置端口映射
    - 容器是隔离环境，外界不可访问，但是可以将宿主机（即 Docker 所在的 Linux 系统）端口映射到容器内端口，当访问宿主机指定端口时，就是在访问容器内的端口。
    - 容器内端口往往是由容器内的进程决定，例如 MySQL 进程默认端口是 3306，因此容器内端口就是
      3306；而宿主机端口则可以任意指定，一般与容器内保持一致。
    - 格式： -p 宿主机端口:容器内端口，该命令就是将宿主机的 3306 映射到容器内的 3306 端口
- -e TZ=Asia/Shanghai : 配置容器内进程运行时的一些参数
    - 格式：-e KEY=VALUE，KEY 和 VALUE 都由容器内进程决定
    - 案例中，TZ=Asia/Shanghai 是设置时区；MYSQL_ROOT_PASSWORD=123 是设置 MySQL 的默认密码
- mysql : 设置镜像名称，Docker 会根据这个名字搜索并下载镜像
    - 格式：REPOSITORY:TAG，例如 mysql:8.0，其中 REPOSITORY 可以理解为镜像名，TAG 是版本号
    - 在未指定 TAG 的情况下，默认是最新版本，也就是 mysql:latest

执行命令后，Docker 就会自动搜索并下载 MySQL，然后会自动运行 MySQL。而且，这种安装方式不用考虑运行的操作系统环境，它不仅可以在
CentOS 系统这样安装，
在 Ubuntu 系统、macOS 系统、甚至是装了 WSL 的 Windows 下，都可以使用这条命令来安装
MySQL。如果是手动安装，就需要手动解决安装包不同、环境不同的、配置不同的问题。
因为 Docker 安装 MySQL 不是直接下载它，而是拉取一个镜像，该镜像中不仅包含了 MySQL 本身，还包含了运行所需要的环境、配置、系统级函数库。基于此，
它在运行时就有自己独立的环境，可以跨系统运行，也不需要手动配置环境，这种独立运行的隔离环境被称为容器。

Docker
官方提供了一个专门管理、存储镜像的网站，并对外开放了镜像上传、下载的权利：[https://hub.docker.com/](https://hub.docker.com/)。
DockerHub 网站是官方仓库，阿里云、华为云会提供一些第三方仓库，也可以自己搭建私有的镜像仓库。

****

## 2. Docker 基础

官方文档：[https://docs.docker.com/](https://docs.docker.com/)

### 2.1 常见命令

| 命令             | 说明                  | 文档地址                                                                            |
|----------------|---------------------|---------------------------------------------------------------------------------|
| docker pull    | 拉取镜像                | [docker pull](https://docs.docker.com/engine/reference/commandline/pull/)       |
| docker push    | 推送镜像到DockerRegistry | [docker push](https://docs.docker.com/engine/reference/commandline/push/)       |
| docker images  | 查看本地镜像              | [docker images](https://docs.docker.com/engine/reference/commandline/images/)   |
| docker rmi     | 删除本地镜像              | [docker rmi](https://docs.docker.com/engine/reference/commandline/rmi/)         |
| docker run     | 创建并运行容器（不能重复创建）     | [docker run](https://docs.docker.com/engine/reference/commandline/run/)         |
| docker stop    | 停止指定容器              | [docker stop](https://docs.docker.com/engine/reference/commandline/stop/)       |
| docker start   | 启动指定容器              | [docker start](https://docs.docker.com/engine/reference/commandline/start/)     |
| docker restart | 重新启动容器              | [docker restart](https://docs.docker.com/engine/reference/commandline/restart/) |
| docker rm      | 删除指定容器              | [docker rm](https://docs.docker.com/engine/reference/commandline/rm/)           |
| docker ps      | 查看容器                | [docker ps](https://docs.docker.com/engine/reference/commandline/ps/)           |
| docker logs    | 查看容器运行日志            | [docker logs](https://docs.docker.com/engine/reference/commandline/logs/)       |
| docker exec    | 进入容器                | [docker exec](https://docs.docker.com/engine/reference/commandline/exec/)       |
| docker save    | 保存镜像到本地压缩文件         | [docker save](https://docs.docker.com/engine/reference/commandline/save/)       |
| docker load    | 加载本地压缩文件到镜像         | [docker load](https://docs.docker.com/engine/reference/commandline/load/)       |
| docker inspect | 查看容器详细信息            | [docker inspect](https://docs.docker.com/engine/reference/commandline/inspect/) | 

Docker 的核心命令可以划分为三个主要环节：镜像构建与管理、镜像仓库交互、容器生命周期管理：

通过 docker build 命令，可以基于 Dockerfile 构建出一个自定义的镜像，比如定制版的 Nginx 服务镜像。构建好的镜像可以使用
docker images 查看详细信息，
如镜像名、标签、大小等；如果不再使用某个镜像，可以使用 docker rmi 删除它。Docker 还提供了离线共享机制 docker save 和 docker
load：
前者将镜像打包为 .tar 文件，后者可从文件中恢复出镜像，实现离线迁移。

关于镜像仓库交互操作，使用 docker pull 可以从远程镜像仓库（如 Docker Hub）拉取镜像到本地，比如拉取官方提供的 MySQL 镜像；docker
push 则可以将本地构建好的镜像上传到仓库，这一过程类似源代码的版本管理。

通过 docker run，可以基于镜像创建并启动一个新的容器，例如使用 Nginx 镜像启动一个 Web 服务。运行中的容器可以通过 docker stop
停止，
再用 docker start 重新启动，或者直接使用 docker restart。docker ps 可以查看当前正在运行的容器列表，了解容器状态和端口映射等情况（加上
-a 则是查看所有）。
docker logs 则用于查看容器的运行日志；而 docker exec 可以进入容器内部执行命令，比如修改配置或检查进程。当容器不再使用时，可以用
docker rm 将其删除。

默认情况下，每次重启虚拟机都需要手动启动 Docker 和 Docker 中的容器。通过命令可以实现开机自启：

```shell
# Docker 开机自启
systemctl enable docker

# Docker 容器开机自启
docker update --restart=always [容器名/容器id]
```

配置一个 Nginx 镜像：

1、去 DockerHub 查看 [nginx](https://docs.docker.com/engine/swarm/configs/) 镜像仓库及相关信息

2、拉取 Nginx 镜像

```shell
# 默认拉取最新版
docker pull nginx
# 打印结果如下：
Using default tag: latest
latest: Pulling from library/nginx
59e22667830b: Pull complete
140da4f89dcb: Pull complete
96e47e70491e: Pull complete
2ef442a3816e: Pull complete
4b1e45a9989f: Pull complete
1d9f51194194: Pull complete
f30ffbee4c54: Pull complete
Digest: sha256:84ec96...
Status: Downloaded newer image for nginx:latest
docker.io/library/nginx:latest
```

3、查看镜像

```shell
docker images
# 结果如下：
REPOSITORY           TAG       IMAGE ID       CREATED        SIZE
nginx                latest    2cd1d97f893f   9 days ago     192MB
redis                latest    ed3a2af6d0d4   7 weeks ago    128MB
hello-world          latest    74cc54e27dc4   6 months ago   10.1kB
canal/canal-server   latest    c5915ee0bdab   6 months ago   1.69GB
mysql                8.0.33    f6360852d654   2 years ago    565MB
```

4、创建并允许 Nginx 容器

```shell
docker run -d \
    --name nginx \
    -p 80:80 \
    nginx
# 返回 id 表示成功
70a1118...
```

5、查看运行中容器

```shell
docker ps
# 也可以加格式化方式访问，格式会更加清爽
docker ps --format "table {{.ID}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}\t{{.Names}}"
# 打印结果如下：
CONTAINER ID   IMAGE          PORTS                                                    STATUS          NAMES
70a111875174   nginx          0.0.0.0:80->80/tcp, [::]:80->80/tcp                      Up 35 seconds   nginx
21c54622906e   mysql:8.0.33   33060/tcp, 0.0.0.0:3307->3306/tcp, [::]:3307->3306/tcp   Up 2 hours      mysql
```

6、访问网页，地址：http://localhost

7、查看容器详细信息

```shell
docker inspect nginx
```

8、进入容器，查看容器内目录

```shell
docker exec -it nginx bash
# 终端显示变成：
root@70a111875174:/#
```

9、停止容器

```shell
docker stop nginx
```

10、删除容器

```shell
docker rm nginx
```

有些 Docker 的命令较长，可以采取起别名的方式简化命令：

1、修改 /root/.bashrc 文件

```shell
vi /root/.bashrc
```

2、添加命令与别名

```shell
alias rm='rm -i'
alias cp='cp -i'
alias mv='mv -i'
alias dps='docker ps --format "table {{.ID}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}\t{{.Names}}"'
alias dis='docker images'

# Source global definitions
if [ -f /etc/bashrc ]; then
        . /etc/bashrc
fi
```

3、让文件生效

```shell
source /root/.bashrc
```

****

### 2.2 数据卷

容器是隔离环境，容器内程序的文件、配置、运行时产生的容器都在容器内部，如果要读写容器内的文件就非常不方便。一般情况下，应该是要遵循容器运行环境应与数据、配置解耦。
容器的本质是轻量级、快速启动、易于销毁的运行环境，这就意味着容器生命周期短，随时可能被销毁或替换，因此程序的数据（如 MySQL
的数据库文件）、配置（如 nginx.conf）、资源（如静态资源） 不能直接放在容器里。

数据卷是 Docker 主机上的一个目录或文件，它可以被挂载到容器中。与容器的可写层不同，数据卷的数据不会随着容器的删除而丢失，并且对数据卷的修改会立即生效。它主要有以下几个作用：

- 数据持久化：使用数据卷后，即使容器被删除，数据卷中的数据依然保留在主机上，下次启动新容器时可以继续使用。
- 数据共享：多个容器可以同时挂载同一个数据卷，实现数据的共享。例如一个 Web 应用容器和一个数据库容器共享存储用户上传文件的目录。
- 简化配置：可以将配置文件放在数据卷中，在不同的容器中挂载相同的数据卷，这样就可以快速复用相同的配置，而无需在每个容器中单独配置。

相关命令：

| 命令                    | 说明         | 文档地址                                                                                          |
|-----------------------|------------|-----------------------------------------------------------------------------------------------|
| docker volume create  | 创建数据卷      | [docker volume create](https://docs.docker.com/engine/reference/commandline/volume_create/)   |
| docker volume ls      | 查看所有数据卷    | [docker volume ls](https://docs.docker.com/engine/reference/commandline/volume_ls/)           |
| docker volume rm      | 删除指定数据卷    | [docker volume rm](https://docs.docker.com/engine/reference/commandline/volume_rm/)           |
| docker volume inspect | 查看某个数据卷的详情 | [docker volume inspect](https://docs.docker.com/engine/reference/commandline/volume_inspect/) |
| docker volume prune   | 清除数据卷      | [docker volume prune](https://docs.docker.com/engine/reference/commandline/volume_prune/)     | 

注意：容器与数据卷的挂载要在创建容器时配置，对于创建好的容器，是不能设置数据卷的，而且创建容器的过程中，数据卷会自动创建。

****

#### 1. 挂载数据卷

例如 nginx 的 html 目录挂载：

1、首先创建容器并指定数据卷，通过 -v 参数来指定数据卷

```shell
docker run -d --name nginx -p 80:80 -v html:/usr/share/nginx/html nginx
```

2、查看数据卷

```shell
docker volume ls
# 打印结果：
DRIVER    VOLUME NAME
...
local     html
```

3、查看数据卷详情

```shell
docker volume inspect html

[
    {
        "CreatedAt": "2025-07-24T16:14:35+08:00",
        "Driver": "local",
        "Labels": null,
        "Mountpoint": "/var/lib/docker/volumes/html/_data",
        "Name": "html",
        "Options": null,
        "Scope": "local"
    }
]
```

4、查看 /var/lib/docker/volumes/html/_data 目录

```shell
ll /var/lib/docker/volumes/html/_data

total 8
-rw-r--r-- 1 root root 497 Jun 25 01:22 50x.html
-rw-r--r-- 1 root root 615 Jun 25 01:22 index.html
```

5、进入该目录，并随意修改 index.html 内容

```shell
# 获取一个 root shell，切换到 root 用户
sudo -s
cd /var/lib/docker/volumes/html/_data
vi index.html
```

****

#### 2. 匿名数据卷

先查看一下 MySQL 容器的详细信息：

```shell
docker inspect mysql
```

关注其中 .Config.Volumes 部分和 .Mounts 部分，可以发现这个容器声明了一个本地目录，需要挂载数据卷，但是数据卷未定义，这就是匿名卷。

```shell
{
    "Config": {
        "Volumes": {
            "/var/lib/mysql": {}
        },
    },
}
```

- /var/lib/mysql：容器内的路径，即 MySQL 默认的数据目录
- {}：空对象，表示这个路径会被 Docker 用数据卷挂载

```shell
{
  "Mounts": [
        {
            "Type": "volume",
            "Name": "278e740c8aea5cfa51ab666ea44dd9e5d0ffce3eb35e80214058e472a6cbcdac",
            "Source": "/var/lib/docker/volumes/278e740c8aea5cfa51ab666ea44dd9e5d0ffce3eb35e80214058e472a6cbcdac/_data",
            "Destination": "/var/lib/mysql",
            "Driver": "local",
            "Mode": "",
            "RW": true,
            "Propagation": ""
        }
    ],
}
```

Mounts 中有几个关键属性：

- Name：数据卷名称，由于定义容器未设置容器名，这里的就是匿名卷自动生成的名字，一串 hash 值。
- Source：宿主机目录
- Destination：容器内的目录

上述配置是将容器内的 /var/lib/mysql 这个目录，与数据卷 278e740c8... 挂载，于是在宿主机中就有了
/var/lib/docker/volumes/278e740c8... 这个目录。
这就是匿名数据卷对应的目录，它的使用方式与普通数据卷没有差别。即使没有显式挂载数据卷，Docker 也会自动挂载一个匿名数据卷。因为
MySQL 镜像的 Dockerfile 中定义了：

```shell
VOLUME /var/lib/mysql
```

这个声明表示：在容器运行时，该路径将使用数据卷存储。为什么 Docker 会选择自动挂载呢？因为容器特有的性质导致存储在容器中的配置、运行环境等会随着容器的删除而丢失，
为了保证像 MySQL 这样的数据库应用中的数据（如数据库表结构、存储的业务数据等）能够长期保存，不受容器生命周期的影响，就采取了自动挂载匿名数据卷的形式。

查看该目录下的 MySQL 的 data 文件：

```shell
ls -l /var/lib/docker/volumes/278e740c8.../_data
```

****

#### 3. 挂载本地目录或文件

数据卷的目录结构较复杂，如果直接操作数据卷目录会不太方便。大多情况下，应该直接将容器目录与宿主机指定目录挂载，或者直接挂载到
Windows 磁盘中。
挂载语法与数据卷类似：

```shell
# 挂载宿主机本地目录
-v 本地目录:容器内目录
# 挂载 Windows 本地目录
-v 本地文件:容器内文件
```

例如：

```shell
# 挂载宿主机本地目录
docker run -v /home/user/mysql-data:/var/lib/mysql mysql
# 挂载 Windows 本地目录
docker run -v D:\docker\mysql-data:/var/lib/mysql mysql
```

现在尝试将本地 Windows 目录挂载到容器内，并且使用对应的初始化 SQL
脚本和配置文件，官方文档：[mysql](https://hub.docker.com/_/mysql)。

容器的默认 mysql 配置文件目录为：/etc/mysql/conf.d，将 Windows 磁盘的目录挂载到这就行；
初始化 SQL 脚本的默认目录为：/docker-entrypoint-initdb.d；
mysql 数据存储的默认目录为：/var/lib/mysql。

所以将 Windows 磁盘目录挂载到容器的对应路径为：

- 挂载 mysql_data_volume 到容器内的 /var/lib/mysql 目录
- 挂载 D:\docker_dataMountDirectory\mysql\init 到容器内的 /docker-entrypoint-initdb.d 目录（初始化的SQL脚本目录）
- 挂载 D:\docker_dataMountDirectory\mysql\conf 到容器内的 /etc/mysql/conf.d 目录（这个是 MySQL 配置文件目录）

```shell
docker run -d \
  --name mysql \
  -p 3306:3306 \
  -e TZ=Asia/Shanghai \
  -e MYSQL_ROOT_PASSWORD=123 \
  -v ./mysql/data:/var/lib/mysql \
  -v /mnt/d/docker_dataMountDirectory/mysql/conf:/etc/mysql/conf.d \
  -v /mnt/d/docker_dataMountDirectory/mysql/init:/docker-entrypoint-initdb.d \
  mysql:8.0.33
```

需要注意的是：/var/lib/mysql 是 MySQL 容器中最核心的数据目录，主要用来存放：

- 数据库的所有数据文件，包括表数据、索引、事务日志、二进制日志等
- 系统表和元数据，MySQL 自身的管理信息也保存在这里。
- 数据库的 socket 文件和配置相关文件（部分）

所以建议：

- 在 Windows 上使用 WSL2 或 Docker Desktop 挂载 Windows 目录时，Linux 容器往往无法正确操作 Windows 文件系统的权限，导致类似
  “Operation not permitted” 的错误。
- 所以建议避免直接挂载 Windows 目录到 MySQL 的数据目录，只把配置文件和初始化脚本挂载到 Windows 目录，数据目录使用 Docker
  卷，让 Docker 维护数据存储在其内部虚拟文件系统里。
- WSL2 中挂载 Windows 盘时，路径要用 Linux 格式，比如
  /mnt/d/...；上述命令的路径写法为：/mnt/d/docker_dataMountDirectory/mysql/conf:/etc/mysql/conf.d

具体操作：

1、删除原来的 MySQL 容器

2、创建并运行新 mysql 容器，挂载本地目录，使用上面的那个命令

3、查看目录，检查是否创建了 /mysql/data

```shell
# 查看当前目录
pwd
# 打印结果
/home/cell
# 查看当前目录下的文件
ls -l
# 打印结果
total 114408
...
drwxr-xr-x 3 root    root ... mysql
# 查看 mysql 文件
ls -l mysql
# 打印结果
total 4
drwxr-xr-x 8 dnsmasq root ... data
# 查看 data 文件
ls -l data
...
```

4、查看 MySQL 容器内数据

```shell
docker exec -it mysql2 mysql -uroot -p123
# 进入 mysql 容器
mysql> 
# 查看编码表（本地挂载的配置文件中配置了编码）
show variables like "%char%";
# 打印结果
+--------------------------+--------------------------------+
| Variable_name            | Value                          |
+--------------------------+--------------------------------+
| character_set_client     | utf8mb4                        |
| character_set_connection | utf8mb4                        |
| character_set_database   | utf8mb4                        |
| character_set_filesystem | binary                         |
| character_set_results    | utf8mb4                        |
| character_set_server     | utf8mb4                        |
| character_set_system     | utf8mb3                        |
| character_sets_dir       | /usr/share/mysql-8.0/charsets/ |
+--------------------------+--------------------------------+
# 查看数据库
show databases;
+--------------------+
| Database           |
+--------------------+
| hmall              |
| information_schema |
| mysql              |
| performance_schema |
| sys                |
+--------------------+
...
```

5、用 Navicat 连接测试，可以发现有对应的数据库存在。

基于以上操作，完成本地目录的挂在后，即使删除了容器，本地目录内的数据是不会丢失的，容器里 /var/lib/mysql
所有的文件操作，都会映射到宿主机的挂载路径上；
同理，只要使用这些本地目录进行挂载，那么就可以达到数据恢复的操作。

****

### 2.3 镜像

#### 1. 概念

镜像（Image）是 Docker 容器的只读模板，它包含了运行某个应用所需的所有内容，包括：

- 操作系统环境（比如 Ubuntu、Alpine）
- 预装的软件（如 Nginx、MySQL、Java）
- 配置文件
- 环境变量
- 入口脚本等

镜像类似 Java 的 .class 文件，容器就是镜像运行后形成的实际进程（加上可读写层）。Docker 镜像是由多层（Layer）叠加而成的，每一层都是只读的。
每一条 Dockerfile 指令（如 RUN, COPY, ADD）都会生成一个新的只读层。

例如：

```shell
# 这段代码是一个 Dockerfile，用于构建一个基于 Ubuntu 20.04 的 Nginx Web 服务器镜像。
FROM ubuntu:20.04 # 指定基础镜像，相当于 “从哪个操作系统开始构建”
RUN apt-get update # 更新 Ubuntu 的软件包索引（类似刷新应用商店的列表）
RUN apt-get install -y nginx # 安装 Nginx Web 服务器
COPY index.html /usr/share/nginx/html/ # 将本地的 index.html 文件复制到镜像中的 Nginx 默认网站目录
```

当重新构建镜像时，如果前几层没有变化，它们会被缓存，不会重新构建，这也是镜像的共享机制。

由于制作镜像的过程中，需要逐层处理和打包，比较复杂，所以 Docker 就提供了自动打包镜像的功能。
只需要将打包的过程，每一层要做的事情用固定的语法写下来，交给 Docker 去执行即可，而这种记录镜像结构的文件就称为
Dockerfile，它是一个包含了一系列命令的脚本，这些命令按照顺序执行并生成最终的镜像。
官方文档：[https://docs.docker.com/engine/reference/builder/](https://docs.docker.com/engine/reference/builder/)

常用命令：

| 指令         | 说明                         | 示例                          |
|------------|----------------------------|-----------------------------|
| FROM       | 指定基础镜像                     | FROM centos:6               |
| ENV        | 设置环境变量，可在后面指令使用            | ENV key value               |
| COPY       | 拷贝本地文件到镜像的指定目录             | COPY ./xx.jar /tmp/app.jar  |
| RUN        | 执行Linux的shell命令，一般是安装过程的命令 | RUN yum install gcc         |
| EXPOSE     | 指定容器运行时监听的端口，是给镜像使用者看的     | EXPOSE 8080                 |
| ENTRYPOINT | 镜像中应用的启动命令，容器运行时调用         | ENTRYPOINT java -jar xx.jar |

****

#### 2. 自定义镜像

自定义一个 Java 应用的镜像，在 D:\docker_dataMountDirectory\my_java_demo 下准备两个文件：

- docker-demo.jar：Java 应用的 jar 包
- Dockerfile：镜像的构建脚本

```shell
# 基础镜像
FROM openjdk:11.0-jre-buster
# 设定时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
# 拷贝 jar 包
COPY docker-demo.jar /app.jar
# 入口，启动时执行的命令
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

在 wsl2 中进入该磁盘：

```shell
cd /mnt/d/docker_dataMountDirectory/my_java_demo
```

然后构建镜像：

```shell
docker build -t my-java-app .
```

- docker build：就是构建一个 docker 镜像
- -t my-java-app：-t 参数是指定镜像的名称，也可以在后面添加 : 1.0（repository 和 tag，指定版本号）
- . : 最后的点是指构建时 Dockerfile 所在路径，由于进入了 D 盘目录，所以指定的是 . 来代表当前目录

验证镜像：

```shell
docker images
# 打印结果：
REPOSITORY           TAG       IMAGE ID       CREATED         SIZE
...
my-java-app          latest    d77979e0289f   2 minutes ago   315MB
```

运行镜像：

```shell
docker run -d --name my-java-demo -p 8080:8080 my-java-app
# 打印结果：
2bc56c92449f4a7b1a273f886144fa1ab17a337f902e4c6928fd5fe0cef4fd6d
```

访问地址：

```shell
curl localhost:8080/hello/count
# 打印结果：
<h5>欢迎访问黑马商城, 这是您第6次访问<h5>
```

****

### 2.4 网络

上面创建了一个 Java 项目的容器，而 Java 项目往往需要访问其它各种中间件，例如 MySQL、Redis 等。而 Docker 默认为所有容器创建一个叫作
bridge 的默认网络（除非显式使用 --network）。
在同一 bridge 网络下的容器，可以通过容器名互相通信。

查看 mysql2 容器的网络 IP 地址：

```shell
"Networks": {
    "bridge": {
        "IPAMConfig": null,
        "Links": null,
        "Aliases": null,
        "MacAddress": "4a:e1:4c:87:c5:47",
        "DriverOpts": null,
        "GwPriority": 0,
        "NetworkID": "c079483eeaa0e54b270543eacaec9fe041fa789891a18c690ca15d2c809a5e42",
        "EndpointID": "c2f7db20ace5ccdf25f715c605818fe230c9eaf714c2228c462fdf686a0ecec9",
        "Gateway": "172.17.0.1",
        "IPAddress": "172.17.0.3",
        "IPPrefixLen": 16,
        "IPv6Gateway": "",
        "GlobalIPv6Address": "",
        "GlobalIPv6PrefixLen": 0,
        "DNSNames": null
    }
}
```

得到 IP 地址为 172.17.0.3，然后进入 my-java-app 容器，在该容器内通过 ping 命令测试网络：

```shell
docker exec -it my-java-demo bash
ping 172.17.0.3
```

但是，容器的网络 IP 其实是一个虚拟的 IP，其值并不固定与某一个容器绑定，如果在开发时写死某个 IP，而在部署时很可能 MySQL 容器的
IP 会发生变化，连接会失败。常见 Docker 网络的命令：

| 命令                        | 说明           | 文档地址                                                                                                  |
|---------------------------|--------------|-------------------------------------------------------------------------------------------------------|
| docker network create     | 创建一个网络       | [docker network create](https://docs.docker.com/engine/reference/commandline/network_create/)         |
| docker network ls         | 查看所有网络       | [docker network ls](https://docs.docker.com/engine/reference/commandline/network_ls/)                 |
| docker network rm         | 删除指定网络       | [docker network rm](https://docs.docker.com/engine/reference/commandline/network_rm/)                 |
| docker network prune      | 清除未使用的网络     | [docker network prune](https://docs.docker.com/engine/reference/commandline/network_prune/)           |
| docker network connect    | 使指定容器连接加入某网络 | [docker network connect](https://docs.docker.com/engine/reference/commandline/network_connect/)       |
| docker network disconnect | 使指定容器连接离开某网络 | [docker network disconnect](https://docs.docker.com/engine/reference/commandline/network_disconnect/) |
| docker network inspect    | 查看网络详细信息     | [docker network inspect](https://docs.docker.com/engine/reference/commandline/network_inspect/)       | 

自定义 bridge 网络：

1、首先通过命令创建一个网络

```shell
docker network create mynet

82a90caaf846477e6ce8c577020b1738a7d46660dc87d6e89b04e5444021a234
```

2、查看网络

```shell
docker network ls
# 除了 mynet 以外，其它都是默认的网络
NETWORK ID     NAME      DRIVER    SCOPE
c079483eeaa0   bridge    bridge    local
74fb3e77ba1f   host      host      local
82a90caaf846   mynet     bridge    local
```

3、让 my-java-demo 和 mysql2 都加入该网络，注意在加入网络时可以通过 --alias 给容器起别名，可用容器名作为 DNS

```shell
docker network connect mynet mysql2 --alias db
docker network connect mynet my-java-demo
```

4、查看网络信息

```shell
docker network inspect mynet
[
    {
        "Name": "mynet",
        "Id": "82a90caaf846477e6ce8c577020b1738a7d46660dc87d6e89b04e5444021a234",
        ...
            "Config": [
                {
                    "Subnet": "172.19.0.0/16",
                    "Gateway": "172.19.0.1"
                }
            ]
        },
        "Containers": {
            "2bc56c92449f4a7b1a273f886144fa1ab17a337f902e4c6928fd5fe0cef4fd6d": {
                "Name": "my-java-demo",
                "EndpointID": "7d7bea676c53efa1b18a00615c16775a684bf8a0cca86830cc55cb709890307e",
                "MacAddress": "ca:91:31:3e:8a:36",
                "IPv4Address": "172.19.0.3/16",
                "IPv6Address": ""
            },
            "9cf7e5215100f796e64b571ad3d5af91414e496418a810fd104d056c1be063cd": {
                "Name": "mysql2",
                "EndpointID": "51d8696282fffdbf8ef91be02863a9ba9b936cde5fc19771910a43ea5bdd97c6",
                "MacAddress": "86:67:bb:60:6e:5e",
                "IPv4Address": "172.19.0.2/16",
                "IPv6Address": ""
            }
        },
    }
]
```

5、进入 my-java-demo 容器，尝试利用荣启铭和别名访问 mysql2

```shell
docker exec -it my-java-demo bash
ping db
ping mysql2
```

- 在自定义网络中，可以给容器起多个别名，默认的别名是容器名本身
- 在同一个自定义网络中的容器，可以通过别名互相访问

****

## 3. 项目部署

### 3.1 部署 Java 项目

先创建网络，用于连接 Java 项目和 mysql：

```shell
docker network create hm-net
```

把创建好的 mysql 容器接入到网络中：

```shell
docker network connect hm-net mysql
```

hmall 项目是一个 maven 聚合项目，使用 IDEA 打开 hmall 项目，它有两个子模块：一个 hm-common，一个 hm-service，需要进行部署的就是
hm-service；
因为 hm-common 模块本身不包含业务逻辑，也没有启动类，不能独立运行，在每个服务模块引入了 common 依赖，意味着在 maven
编译打包服务模块时，
hm-common 的代码会被一起编译进去，打包进 jar 包内。

打包完成后，将 hm-service 目录下的 Dockerfile 和 hm-service/target 目录下的 hm-service.jar 部署到 docker 中：

```shell
# 在 wsl2 中进入该磁盘
cd /mnt/d/docker_dataMountDirectory/hmall
# 构建镜像
docker build -t hmall .
```

****

### 3.3 DockerCompose

部署一个简单的项目通常需要 3 个容器：

- MySQL
- Nginx
- Java 项目

但实际项目中不止这些，所以使用 Docker Compose 可以帮助实现多个相互关联的 Docker 容器的快速部署，它允许用户通过一个单独的
docker-compose.yml 模板文件（YAML 格式）来定义一组相关联的应用容器。

#### 3.1 基本语法

docker-compose 文件中可以定义多个相互关联的应用容器，每一个应用容器被称为一个服务（service）。由于 service 就是在定义某个应用的运行时参数，
因此与 docker run 参数非常相似。用 docker ru n部署 MySQL 的命令如下：

```shell
docker run -d \
  --name mysql \
  -p 3306:3306 \
  -e TZ=Asia/Shanghai \
  -e MYSQL_ROOT_PASSWORD=123 \
  -v ./mysql/data:/var/lib/mysql \
  -v ./mysql/conf:/etc/mysql/conf.d \
  -v ./mysql/init:/docker-entrypoint-initdb.d \
  --network hmall
  mysql
```

如果用 docker-compose.yml 文件来定义，就是这样：

```shell
version: "3.8"

services:
  mysql:
    image: mysql
    container_name: mysql
    ports:
      - "3306:3306"
    environment:
      TZ: Asia/Shanghai
      MYSQL_ROOT_PASSWORD: 123
    volumes:
      - "./mysql/conf:/etc/mysql/conf.d"
      - "./mysql/data:/var/lib/mysql"
    networks:
      - new
networks:
  new:
    name: hmall
```

对比如下：

| docker run 参数 | docker compose 指令 | 说明    |
|:-------------:|:-----------------:|-------|
|    --name     |  container_name   | 容器名称  |
|      -p       |       ports       | 端口映射  |
|      -e       |    environment    | 环境变量  |
|      -v       |      volumes      | 数据卷配置 |
|   --network   |     networks      | 网络    |

****

#### 3.2 基础命令

```shell
docker compose [OPTIONS] [COMMAND]
```

其中，OPTIONS 和 COMMAND 都是可选参数，比较常见的有：

| 类型       | 参数或指令   | 说明                                                     |
|----------|---------|--------------------------------------------------------|
| Options  | -f      | 指定compose文件的路径和名称                                      |
| Options  | -p      | 指定project名称。project就是当前compose文件中设置的多个service的集合，是逻辑概念 |
| Commands | up      | 创建并启动所有service容器                                       |
| Commands | down    | 停止并移除所有容器、网络                                           |
| Commands | ps      | 列出所有启动的容器                                              |
| Commands | logs    | 查看指定容器的日志                                              |
| Commands | stop    | 停止容器                                                   |
| Commands | start   | 启动容器                                                   |
| Commands | restart | 重启容器                                                   |
| Commands | top     | 查看运行的进程                                                |
| Commands | exec    | 在指定的运行中容器中执行命令                                         |

****

# 三、微服务

## 1. 概述

### 1.1 单体架构

单体架构（monolithic structure）就是整个项目中所有功能模块都在一个工程中开发；项目部署时需要对所有模块一起编译、打包；项目的架构设计、开发模式都非常简单。
当项目规模较小时，这种模式上手快，部署、运维也都很方便，因此早期很多小型项目都采用这种模式。但随着项目的业务规模越来越大，团队开发人员也不断增加，单体架构就呈现出越来越多的问题：

- 团队协作成本高：由于所有模块都在一个项目中，不同模块的代码之间物理边界越来越模糊，最终要把功能合并到一个分支，此时可能发生各种
  bug，导致解决问题较为麻烦。
- 系统发布效率低：任何模块变更都需要发布整个系统，而系统发布过程中需要多个模块之间制约较多，需要对比各种文件，任何一处出现问题都会导致发布失败，往往一次发布需要数十分钟甚至数小时。
- 系统可用性差：单体架构各个功能模块是作为一个服务部署，相互之间会互相影响，一些热点功能会耗尽系统资源，导致其它服务低可用。

例如访问下面两个接口：

- http://localhost:8080/hi
- http://localhost:8080/search/list

经过测试，目前 /search/list 是比较正常的，访问耗时在 30 毫秒左右。但如果此时 /hi
接口称为一个并发较高的热点接口，他就会抢占大量资源，最终会有越来越多请求积压，直至Tomcat资源耗尽。
其它本来正常的接口（例如/search/list）也都会被拖慢，甚至因超时而无法访问了。

****

### 1.2 微服务

微服务架构，首先是服务化，就是将单体架构中的功能模块从单体应用中拆分出来，独立部署为多个服务，同时要满足下面的一些特点：

- 单一职责：一个微服务负责一部分业务功能，并且其核心数据不依赖于其它模块。
- 团队自治：每个微服务都有自己独立的开发、测试、发布、运维人员，团队人员规模不超过10人
- 服务自治：每个微服务都独立打包部署，访问自己独立的数据库。并且要做好服务隔离，避免对其它服务产生影响

例如商城项目，就可以把商品、用户、购物车、交易等模块拆分，交给不同的团队去开发，并独立部署。微服务架构解决了单体架构存在的问题：

- 由于服务拆分，每个服务代码量大大减少，参与开发的后台人员在1~3名，协作成本大大降低
- 每个服务都是独立部署，当有某个服务有代码变更时，只需要打包部署该服务即可
- 每个服务独立部署，并且做好服务隔离，使用自己的服务器资源，不会影响到其它服务。

****

## 2. 微服务拆分

### 2.1 服务拆分原则

#### 1. 什么时候拆

一般情况下，对于一个初创的项目，首先要做的是验证项目的可行性。因此这一阶段的首要任务是敏捷开发，快速产出生产可用的产品，投入市场做验证。
为了达成这一目的，该阶段项目架构往往会比较简单，很多情况下会直接采用单体架构，这样开发成本比较低，可以快速产出结果，一旦发现项目不符合市场，损失较小。
如果这一阶段采用复杂的微服务架构，投入大量的人力和时间成本用于架构设计，最终发现产品不符合市场需求，等于全部做了无用功。
所以，对于大多数小型项目来说，一般是先采用单体架构，随着用户规模扩大、业务复杂后再逐渐拆分为微服务架构，这样初期成本会比较低，可以快速试错。
但是，这么做的问题就在于后期做服务拆分时，可能会遇到很多代码耦合带来的问题，拆分比较困难（前易后难）。而对于一些大型项目，在立项之初目的就很明确，为了长远考虑，
在架构设计时就直接选择微服务架构。虽然前期投入较多，但后期就少了拆分服务的烦恼（前难后易）。

****

#### 2. 怎么拆

具体可以从两个角度来分析：

- 高内聚：每个微服务的职责要尽量单一，但包含的业务相互关联度高、完整度高。
- 低耦合：每个微服务的功能要相对独立，尽量减少对其它微服务的依赖，或者依赖接口的稳定性要强。

做服务拆分时一般有两种方式：

- 纵向拆分

按照项目的功能模块来拆分。例如黑马商城中有用户管理功能、订单管理功能、购物车功能、商品管理功能、支付功能等。那么按照功能模块将它们拆分为一个个服务，就属于纵向拆分。
这种拆分模式可以尽可能提高服务的内聚性。

- 横向拆分

看各个功能模块之间有没有公共的业务部分，如果有将其抽取出来作为通用服务。例如用户登录是需要发送消息通知，记录风控数据，下单时也要发送短信，记录风控数据。
因此消息发送、风控数据记录就是通用的业务功能，因此可以将它们分别抽取为公共服务：消息中心服务、风控管理服务。这样可以提高业务的复用性，避免重复开发。
同时通用业务一般接口稳定性较强，也不会使服务之间过分耦合。

而黑马商城按照纵向拆分则可以分为以下几个微服务：

- 用户服务
- 商品服务
- 订单服务
- 购物车服务
- 支付服务

****

### 2.2 拆分购物车、商品服务

一般微服务项目有两种不同的工程结构：

- 完全解耦：每一个微服务都创建为一个独立的工程，甚至可以使用不同的开发语言来开发，项目完全解耦。
    - 优点：服务之间耦合度低
    - 缺点：每个项目都有自己的独立仓库，管理起来比较麻烦

- Maven 聚合：整个项目为一个 Project，然后每个微服务是其中的一个 Module
    - 优点：项目代码集中，管理和运维方便
    - 缺点：服务之间耦合，编译时间较长

在 hmall 父工程之中已经提前定义了 SpringBoot、SpringCloud 的依赖版本，所以可以直接在这个项目中创建微服务 module。购物车对应
cart-service，商品服务对应 item-service。
分别导入 controller、service 和 mapper。

****

### 2.3 服务调用

在拆分的时候有一个问题：就是购物车业务中需要查询商品信息，但商品信息查询的逻辑全部迁移到了 item-service
服务，导致无法查询。最终结果就是查询到的购物车数据不完整，
要解决这个问题就必须改造其中的代码，把原本本地方法调用，改造成跨微服务的远程调用（RPC，即 Remote Produce Call）。即修改以下的代码：

```java
// 1. 获取商品id
Set<Long> itemIds = vos.stream().map(CartVO::getItemId).collect(Collectors.toSet());
// 2. 查询商品
List<ItemDTO> items = itemService.queryItemByIds(itemIds);
```

当前端向服务端发送查询数据请求时，其实就是从浏览器远程查询服务端数据。比如通过 Swagger
测试商品查询接口，就是向 http://localhost:8081/items 这个接口发起的请求。
而这种查询就是通过 http 请求的方式来完成的，不仅仅可以实现远程查询，还可以实现新增、删除等各种远程请求。

#### 1. RestTemplate

Spring 提供了一个 RestTemplate 的 API，它是一个用于访问 REST 风格服务的模板类，封装了底层的 HTTP 请求逻辑，
提供了多种方法以此实现 GET、POST、PUT、DELETE 等操作。

常用方法：

- `getForObject()`：发送 GET 请求，返回对象
- `getForEntity()`：发送 GET 请求，返回 ResponseEntity
- `postForObject()`：发送 POST 请求，返回对象
- `postForEntity()`：发送 POST 请求，返回 ResponseEntity
- `put()`：发送 PUT 请求，无返回值
- `delete()`：发送 DELETE 请求
- `exchange()`：更灵活地发送各种请求
- `execute()`：最底层的请求控制方法

在 cart-service 服务中定义一个配置类：

```java

@Configuration
public class RemoteCallConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

修改 cart-service 中的 com.hmall.cart.service.impl.CartServiceImpl 的 handleCartItems 方法，发送 http 请求到
item-service：

```java
private void handleCartItems(List<CartVO> vos) {
    // 1. 获取商品id
    Set<Long> itemIds = vos.stream().map(CartVO::getItemId).collect(Collectors.toSet()); // 使用 toSet() 去重 + 避免重复请求
    // 2. 查询商品，利用 RestTemplate 发起 http 请求，得到 http 的响应
    ResponseEntity<List<ItemDTO>> response = restTemplate.exchange(
            "http://localhost:8081/items?ids={ids}", // 要调用的商品服务接口（支持多 id 查询）
            HttpMethod.GET, // GET 请求
            null, // 不传请求体（GET 请求）
            new ParameterizedTypeReference<List<ItemDTO>>() {
            }, // 因为需要接收的是 ItemDTO 类型的集合，所以需要告诉 RestTemplate 返回值类型是 List<ItemDTO>（Java 泛型反射）
            Map.of("ids", CollUtil.join(itemIds, ","))
    );
    // 解析响应
    if (!response.getStatusCode().is2xxSuccessful()) {
        // 查询失败，直接结束
        return;
    }
    List<ItemDTO> items = response.getBody();
    if (CollUtils.isEmpty(items)) {
        return;
    }
    // 3. 转为 id 到 item 的 map
    Map<Long, ItemDTO> itemMap = items.stream().collect(Collectors.toMap(ItemDTO::getId,
            Function.identity())); // 把当前的元素 ItemDTO 本身作为 Map 的 value
    // 4. 写入 vo
    for (CartVO v : vos) {
        ItemDTO item = itemMap.get(v.getItemId());
        if (item == null) {
            continue;
        }
        v.setNewPrice(item.getPrice());
        v.setStatus(item.getStatus());
        v.setStock(item.getStock());
    }
}
```

需要注意的是，需要使用 RestTemplate 就需要把它注入进来，但 SpringBoot 推荐使用构造方法的方式进行注入，为了避免手写构造方法，所以可以使用
@RequiredArgsConstructor 注解，
它只会为 final 字段或 @NonNull 字段生成构造器，这样就可以避免其它无需 SpringBoot 注入的字段也被构造器一起初始化：

```java

@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements ICartService {
    private final RestTemplate restTemplate;
    ...
}
```

****

## 3. 服务治理

### 3.1 注册中心

在微服务远程调用的过程中，包括两个角色：

- 服务提供者：提供接口供其它微服务访问，比如 item-service
- 服务消费者：调用其它微服务提供的接口，比如 cart-service

在大型微服务项目中，服务提供者的数量会非常多，为了管理这些服务就引入了注册中心的概念。

- 服务启动时就会注册自己的服务信息（服务名、IP、端口）到注册中心
- 调用者可以从注册中心订阅想要的服务，获取服务对应的实例列表（1个服务可能多实例部署）
- 调用者自己对实例列表负载均衡，挑选一个实例
- 调用者向该实例发起远程调用

当服务提供者的实例宕机或者启动新实例时，调用者如何得知呢？

- 服务提供者会定期向注册中心发送请求，报告自己的健康状态（心跳请求）
- 当注册中心长时间收不到提供者的心跳时，会认为该实例宕机，将其从服务的实例列表中剔除
- 当服务有新实例启动时，会发送注册服务请求，其信息会被记录在注册中心的服务实例列表
- 当注册中心服务列表变更时，会主动通知微服务，更新本地服务列表

****

### 3.2 Nacos 注册中心

目前开源的注册中心框架有很多，国内比较常见的有：

- Eureka：Netflix 公司出品，目前被集成在 SpringCloud 当中，一般用于 Java 应用
- Nacos：Alibaba 公司出品，目前被集成在 SpringCloudAlibaba 中，一般用于 Java 应用
- Consul：HashiCorp 公司出品，目前集成在 SpringCloud 中，不限制微服务语言

基于 Docker 来部署 Nacos 的注册中心，要准备 MySQL 数据库表用来存储 Nacos 的数据。由于是 Docker 部署，所以需要将 SQL 文件导入到
Docker 中的 MySQL 容器中。
然后将 nacos/custom.env 文件上传至虚拟机：

```text
PREFER_HOST_MODE=hostname
MODE=standalone
SPRING_DATASOURCE_PLATFORM=mysql
MYSQL_SERVICE_HOST=host.docker.internal
MYSQL_SERVICE_DB_NAME=nacos
MYSQL_SERVICE_PORT=3306
MYSQL_SERVICE_USER=root
MYSQL_SERVICE_PASSWORD=123
MYSQL_SERVICE_DB_PARAM=characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
```

需要注意的是：由于使用的是 WSL2 + Docker，所以容器的 IP 地址可能每天变动，这就说明不能直接使用 IP 访问，所以使用
MYSQL_SERVICE_HOST=host.docker.internal，
让容器访问宿主机的 MySQL。如果能确保容器的 IP 地址不会改变，那也可以使用 IP 访问，但是如果 MySQL 使用的 IP 是 127.0.0.1
的话，也不能直接用这个 IP 让 nacos 连接到 MySQL，
因为它们不处于同一个网络，就会导致 nacos 连接到的不是配置在 Docker 中的 MySQL，而是 nacos 内部的。因为容器里的 127.0.0.1
是容器自己的环回地址，不是宿主机地址。

进入对应的 Windows 磁盘目录后，再执行 docker 命令：

```shell
cd /mnt/d/docker_dataMountDirectory/nacos
```

```shell
docker run -d \
--name nacos \
--env-file ./custom.env \
-p 8848:8848 \
-p 9848:9848 \
-p 9849:9849 \
--restart=always \
nacos/nacos-server:v2.1.0-slim
```

启动完成后，访问下面地址：http://192.168.0.105:8848/nacos/ ，首次访问会跳转到登录页，账号密码都是 nacos。

****

### 3.3 服务注册

完成 nacos 在 docker 中的初始化后，把 item-service 注册到 nacos：

添加依赖：

```xml
<!--nacos 服务注册发现-->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

在 item-service 的 application.yml 中添加 nacos 地址配置：

```yaml
spring:
  application:
    name: item-service # 服务名称
  cloud:
    nacos:
      server-addr: 192.168.150.101:8848 # nacos地址
```

启动服务实例后，访问 nacos 控制台，可以发现服务注册成功：

| 服务名          | 分组名称          | 集群数目 | 实例数 | 健康实例数 | 触发保护阈值 | 操作                      |
|--------------|---------------|------|-----|-------|--------|-------------------------|
| cart-service | DEFAULT_GROUP | 1    | 1   | 1     | false  | 详情 \| 示例代码 \| 订阅者 \| 删除 |
| item-service | DEFAULT_GROUP | 1    | 1   | 1     | false  | 详情 \| 示例代码 \| 订阅者 \| 删除 |

然后服务调用者 cart-service 就可以去订阅 item-service 服务了，不过 item-service
可能有多个实例，而真正发起调用时只需要知道一个实例的地址。所以服务调用者必须利用负载均衡从多个实例中挑选一个去访问。
并且服务发现需要用到一个工具 DiscoveryClient，不过 SpringCloud 已经自动装配，可以直接使用：

```java
private final DiscoveryClient discoveryClient;
...

private void handleCartItems(List<CartVO> vos) {
    // 1. 获取商品id
    Set<Long> itemIds = vos.stream().map(CartVO::getItemId).collect(Collectors.toSet());
    // 2. 查询商品
    // 发现 item-service 服务的实力列表
    List<ServiceInstance> instances = discoveryClient.getInstances("item-service");
    // 使用负载均衡
    ServiceInstance instance = instances.get(RandomUtil.randomInt(instances.size()));
    // 利用 RestTemplate 发起 http 请求，得到 http 的响应
    ResponseEntity<List<ItemDTO>> response = restTemplate.exchange(
            // "http://localhost:8081/items?ids={ids}",
            instance.getUri() + "/items?ids={ids}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<ItemDTO>>() {
            },
            Map.of("ids", CollUtil.join(itemIds, ","))
    );
    ...
}
```

****

### 3.4 OpenFeign

#### 1. 使用

上面利用 Nacos 实现了服务的治理，利用 RestTemplate 实现了服务的远程调用，但是远程调用的代码太复杂了，一会儿远程调用，一会儿本地调用。所以引出了
OpenFeign 组件。
远程调用的关键点：

- 请求方式
- 请求路径
- 请求参数
- 返回值类型

OpenFeign 就利用 SpringMVC 的相关注解来声明上述 4 个参数，然后基于动态代理生成远程调用的代码。使用步骤：

引入依赖：

```xml
<!--openFeign-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
        <!--负载均衡器-->
<dependency>
<groupId>org.springframework.cloud</groupId>
<artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

在 cart-service 的 CartApplication 启动类上添加注解，启动 OpenFeign 功能：

```java

@EnableFeignClients
@MapperScan("com.hmall.cart.mapper")
@SpringBootApplication
public class CartApplication {
}
```

在 cart-service 中，定义一个新的接口，编写 Feign 客户端：

```java

@FeignClient("item-service")
public interface ItemClient {
    @GetMapping("/items")
    List<ItemDTO> queryItemByIds(@RequestParam("ids") Collection<Long> ids);
}
```

这里只需要声明接口，无需实现方法：

- @FeignClient("item-service")：声明服务名称
- @GetMapping ：声明请求方式
- @GetMapping("/items")：声明请求路径
- @RequestParam("ids") Collection<Long> ids ：声明请求参数
- List<ItemDTO> ：返回值类型

配置了上述信息，OpenFeign 就可以利用动态代理实现该方法，并且向 http://item-service/items 发送一个 GET 请求，携带 ids
为请求参数，并自动将返回值处理为 List<ItemDTO>。
然后在 service 层直接调用该接口的方法即可实现远程调用：

```java
private final ItemClient itemClient;
...

private void handleCartItems(List<CartVO> vos) {
    // 1. 获取商品id
    Set<Long> itemIds = vos.stream().map(CartVO::getItemId).collect(Collectors.toSet());
  ...
    List<ItemDTO> items = itemClient.queryItemByIds(itemIds);
    if (CollUtils.isEmpty(items)) {
        return;
    }
  ...
}
```

OpenFeign 完成了服务拉取、负载均衡、发送 http 请求的所有工作，还省去了 RestTemplate 的注册，代码十分便捷。

****

#### 2. 连接池

Feign 是一个声明式的 HTTP 客户端，其底层真正发起 HTTP 请求时，是依赖第三方的 HTTP 客户端库来完成的。其底层支持的 http
客户端实现包括：

- HttpURLConnection：默认实现（jdk 自带），不支持连接池
- Apache HttpClient ：支持连接池
- OKHttp：支持连接池

所以通常会使用带有连接池的客户端来代替默认的 HttpURLConnection，例如 OKHttp。

引入依赖：

```xml
<!--OK http 的依赖 -->
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-okhttp</artifactId>
</dependency>
```

在 application.yml 配置文件中开启 Feign 的连接池功（Spring Boot 3.x 和 Spring Cloud 2023.x 后可以不再写以下配置）：

```yaml
feign:
  okhttp:
    enabled: true # 开启 OKHttp 功能
```

****

#### 3. 抽取公共部分

在拆分 item-service 和 cart-service 两个微服务时，它们里面有部分代码是与需求是一样的，如果此时还要拆分一个
trade-service，它也需要远程调用 item-service 中的根据 id 批量查询商品，
这个功能与 cart-service 中是一样的，所以为了避免大量编写重复的代码，就需要提取它们的公共部分，例如：

- 方案1：抽取到微服务之外的公共 module（即作为一个新的微服务）
- 方案2：每个微服务自己抽取一个 module（即作为微服务的子模块）

方案1抽取更加简单，工程结构也比较清晰，但缺点是整个项目耦合度偏高；方案2抽取相对麻烦，工程结构相对更复杂，但服务之间耦合度降低。在
hmall 下定义一个新的 module，
命名为 hm-api，引入需要使用到的接口依赖、OpenFeign 依赖和负载均衡依赖：

```xml
<!--open feign-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
        <!-- load balancer-->
<dependency>
<groupId>org.springframework.cloud</groupId>
<artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
<dependency>
<groupId>com.github.xiaoymin</groupId>
<artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
<version>4.4.0</version>
</dependency>
```

然后把需要重复使用到的 ItemDTO 和 ItemClient 都放到该模块下，其它需要使用到远程调用功能的地方直接导入 hm-api 包即可，不过需要引入
hm-api 作为依赖：

```xml
<!--feign模块-->
<dependency>
    <groupId>com.heima</groupId>
    <artifactId>hm-api</artifactId>
    <version>1.0.0</version>
</dependency>
```

需要注意的是:因为 ItemClient 现在定义到了 com.hmall.api.client 包下，而 cart-service 的启动类定义在 com.hmall.cart
包下，这就导致扫描不到 ItemClient，
会报错，所以需要：

- 方式1：声明扫描包

```java

@EnableFeignClients(basePackages = "com.hmall.api.client")
@MapperScan("com.hmall.cart.mapper")
@SpringBootApplication
public class CartApplication {
  ...
}
```

- 方式2：声明要用的 FeignClient

```java

@EnableFeignClients(clients = {ItemClient.class})
@MapperScan("com.hmall.cart.mapper")
@SpringBootApplication
public class CartApplication {
  ...
}
```

****

#### 4. 日志配置

OpenFeign 只会在 FeignClient 所在包的日志级别为 DEBUG 时，才会输出日志，而且其日志级别有 4 级：

- NONE：不记录任何日志信息，这是默认值
- BASIC：仅记录请求的方法，URL 以及响应状态码和执行时间
- HEADERS：在 BASIC 的基础上，额外记录了请求和响应的头信息
- FULL：记录所有请求和响应的明细，包括头信息、请求体、元数据

在 hm-api 模块下新建一个配置类，定义 Feign 的日志级别：

```java
public class DefaultFeignConfig {
    @Bean
    public Logger.Level feignLogLevel() {
        return Logger.Level.FULL;
    }
}
```

然后要让这个配置类生效还需要让它被扫描到：

- 局部生效：在某个 FeignClient 中配置，只对当前 FeignClient 生效：

```java

@FeignClient(value = "item-service", configuration = DefaultFeignConfig.class)
public interface ItemClient {
    @GetMapping("/items")
    List<ItemDTO> queryItemByIds(@RequestParam("ids") Collection<Long> ids);
}
```

- 全局生效：在 @EnableFeignClients 中配置，针对所有 FeignClient 生效：

```java

@EnableFeignClients(clients = {ItemClient.class}, defaultConfiguration = DefaultFeignConfig.class)
@MapperScan("com.hmall.cart.mapper")
@SpringBootApplication
public class CartApplication {
    public static void main(String[] args) {
        SpringApplication.run(CartApplication.class, args);
    }
}
```

打印日志：

```text
[ItemClient#queryItemByIds] <--- HTTP/1.1 200 (171ms)
[ItemClient#queryItemByIds] connection: keep-alive
[ItemClient#queryItemByIds] content-type: application/json
[ItemClient#queryItemByIds] date: Mon, 28 Jul 2025 13:41:18 GMT
[ItemClient#queryItemByIds] keep-alive: timeout=60
[ItemClient#queryItemByIds] transfer-encoding: chunked
[ItemClient#queryItemByIds] 
[ItemClient#queryItemByIds] [{"id":100000006163,"name":"巴布豆(BOBDOG)柔薄悦动婴儿拉拉裤XXL码80片(15kg以上)","price":67100,"stock":10000,...}]
[ItemClient#queryItemByIds] <--- END HTTP (369-byte body)
```

****

# 四、网关路由

## 1. 网关

### 1.1 概述

网关是微服务架构中的关键组件，位于客户端与后端服务之间，是网络的关口，数据在网络间传输，从一个网络传输到另一网络时就需要经过网关来做数据的路由转发以及数据安全的校验。
在微服务架构中，客户端不会直接访问每个微服务，而是通过网关作为统一入口。核心功能：

- 身份认证和权限校验：登录校验、JWT 校验、OAuth2、权限路由控制
- 路由转发：根据 URL 或 Header 将请求转发到对应的微服务
- 请求处理：参数校验、统一日志记录、限流、防重放、熔断、降级等
- 协议转换：如 HTTP -> WebSocket、HTTP -> gRPC
- 响应封装：对后端响应进行统一格式包装或脱敏处理
- 监控与日志：埋点数据采集、链路追踪、性能统计、异常报警等
- 限流与熔断：防止服务被高并发压垮，起到“缓冲”的作用

在微服务架构中，微服务数量通常较多，客户端访问不便（需要统一出口），并且多个服务返回的格式、使用的协议可能不同，所以需要一个统一的访问入口来处理这些差异。常见网关:

- Nginx
- Spring Cloud Gateway
- Zuul

****

### 1.2 使用

网关的职责是：请求转发、统一认证、安全校验、限流熔断、日志记录等跨服务通用功能，如果把这些功能混在某一个业务服务里（比如订单服务、用户服务），不仅增加耦合度，还会导致系统维护复杂。
所以应该将这些功能独立成一个网关服务，因此也需要创建一个新的模块来开发功能。

引入依赖：

```xml
<!--网关-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

配置路由：

在 hm-gateway 模块的 resources 目录新建一个 application.yaml 文件：

```yaml
server:
  port: 8080
spring:
  application:
    name: gateway
  cloud:
    nacos:
      server-addr: 192.168.0.105:8848
    gateway:
      routes:
        - id: item # 路由规则id，自定义，唯一
          uri: lb://item-service # 路由的目标服务，lb代表负载均衡，会从注册中心拉取服务列表
          predicates: # 路由断言，判断当前请求是否符合当前规则，符合则路由到目标服务
            - Path=/items/**,/search/** # 这里是以请求路径作为判断规则
        - id: cart
          uri: lb://cart-service
          predicates:
            - Path=/carts/**
        - id: user
          uri: lb://user-service
          predicates:
            - Path=/users/**,/addresses/**
        - id: trade
          uri: lb://trade-service
          predicates:
            - Path=/orders/**
        - id: pay
          uri: lb://pay-service
          predicates:
            - Path=/pay-orders/**
```

在配置文件中

****

### 1.3 路由属性

路由规则的定义语法如下：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: ..
          uri: lb://...
          predicates:
            - Path=/...
```

其中 routes 对应的类型如下：

```java

@ConfigurationProperties("spring.cloud.gateway")
@Validated
public class GatewayProperties {
    public static final String PREFIX = "spring.cloud.gateway";
    private final Log logger = LogFactory.getLog(this.getClass());
    private @NotNull
    @Valid List<RouteDefinition> routes = new ArrayList();
    private List<FilterDefinition> defaultFilters = new ArrayList();
    private List<MediaType> streamingMediaTypes;
    private boolean failOnRouteDefinitionError;
    ...
}
```

routes 是一个集合，也就是说可以定义很多路由规则。而集合中的 RouteDefinition 就是具体的路由规则定义，其中常见的属性如下：

```java

@Validated
public class RouteDefinition {
    private String id;
    private @NotEmpty
    @Valid List<PredicateDefinition> predicates = new ArrayList();
    private @Valid List<FilterDefinition> filters = new ArrayList();
    private @NotNull URI uri;
    private Map<String, Object> metadata = new HashMap();
    private int order = 0;
    ...
}
```

- id：路由的唯一标示
- predicates：路由断言，其实就是匹配条件，满足的才能继续下去
- filters：路由过滤条件
- uri：路由目标地址，lb:// 代表负载均衡，从注册中心获取目标微服务的实例列表，并且负载均衡选择一个访问。

对于 predicates，SpringCloudGateway 中支持的断言类型有很多：

| 名称         | 说明                | 示例                                                                                                     |
|------------|-------------------|--------------------------------------------------------------------------------------------------------|
| After      | 是某个时间点后的请求        | - After=2037-01-20T17:42:47.789-07:00[America/Denver]                                                  |
| Before     | 是某个时间点之前的请求       | - Before=2031-04-13T15:14:47.433+08:00[Asia/Shanghai]                                                  |
| Between    | 是某两个时间点之前的请求      | - Between=2037-01-20T17:42:47.789-07:00[America/Denver], 2037-01-21T17:42:47.789-07:00[America/Denver] |
| Cookie     | 请求必须包含某些cookie    | - Cookie=chocolate, ch.p                                                                               |
| Header     | 请求必须包含某些header    | - Header=X-Request-Id, \d+                                                                             |
| Host       | 请求必须是访问某个host（域名） | - Host=**.somehost.org,**.anotherhost.org                                                              |
| Method     | 请求方式必须是指定方式       | - Method=GET,POST                                                                                      |
| Path       | 请求路径必须符合指定规则      | - Path=/red/{segment},/blue/**                                                                         |
| Query      | 请求参数必须包含指定参数      | - Query=name, Jack或者- Query=name                                                                       |
| RemoteAddr | 请求者的ip必须是指定范围     | - RemoteAddr=192.168.1.1/24                                                                            |
| weight     | 权重处理              |                                                                                                        |

****

## 2. 网关登录校验

单体架构时只需要完成一次用户登录、身份校验，就可以在所有业务中获取到用户信息。而微服务拆分后，每个微服务都独立部署，不再共享数据。也就意味着需要为每个微服务都做登录校验，
这显然不合理，而网关正好又是所有微服务的起点，一切请求都需要先经过网关，那就可以把登录校验的工作放到网关去做，这样：

- 只需要在网关和用户服务保存秘钥
- 只需要在网关开发登录校验功能

但需要注意的是：必须要在网关转发请求到微服务之前就进行校验，否则失去意义。

### 2.1 网关过滤器

网关转发流程：

1. 客户端请求进入网关后由 HandlerMapping 对请求做判断，找到与当前请求匹配的路由规则（Route），然后将请求交给 WebHandler 去处理。
2. WebHandler 则会加载当前路由下需要执行的过滤器链（Filter chain），然后按照顺序逐一执行过滤器。
3. 而 Filter 内部的逻辑分为 pre 和 post 两部分，分别会在请求路由到微服务之前和之后被执行。
4. 只有所有 Filter 的 pre 逻辑都依次顺序执行通过后，请求才会被路由到微服务。
5. 微服务返回结果后，再倒序执行 Filter 的 post 逻辑。
6. 最终把响应结果返回。

最终请求转发是有一个名为 NettyRoutingFilter 的过滤器来执行的，而且这个过滤器是整个过滤器链中顺序最靠后的一个。所以可以定义一个过滤器，在其中实现登录校验逻辑，
并且将过滤器执行顺序定义到 NettyRoutingFilter 之前，这样就可以实现登录校验。

网关过滤器链中的过滤器有两种：

- GatewayFilter：路由过滤器，作用范围比较灵活，可以是任意指定的路由 Route，绑定到某条路由规则上才会生效。比如 /pay-orders/**
  路由的请求才需要做特殊的签名校验，那就只为这个 Route 配置一个 GatewayFilter
- GlobalFilter：全局过滤器，作用范围是所有路由，声明后自动生效。在代码中通过实现 GlobalFilter 接口并注册为 Spring Bean
  后做统一的校验，或者使用 default-filters

常用 Gateway 中内置的 GatewayFilter 过滤器：

| 过滤器名称                  | 作用说明    |
|------------------------|---------|
| `AddRequestHeader`     | 添加请求头   |
| `AddResponseHeader`    | 添加响应头   |
| `RemoveRequestHeader`  | 移除请求头   |
| `RemoveResponseHeader` | 移除响应头   |
| `RewritePath`          | 重写请求路径  |
| `SetStatus`            | 设置返回状态码 |
| `RedirectTo`           | 重定向     |

添加请求头：

```yaml
filters:
  - AddRequestHeader=token, my-custom-token # 设置请求头的名称和内容
```

路径重写：

```yaml
filters:
  - RewritePath=/api/(?<segment>.*), /$\{segment} # 例如访问 /api/user/info 会被转发为 /user/info
```

如果只需要一个路由使用路由器，那就可以把 filters 放到具体的路由下面：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: test_route
          uri: lb://test-service
          predicates:
            -Path=/test/**
          filters:
            - AddRequestHeader=key, value # 逗号之前是请求头的key，逗号之后是value
```

如果需要全局配置，那就可以使用 default-filters：

```yaml
spring:
  cloud:
    gateway:
      default-filters: # default-filters 下的过滤器可以作用于所有路由
        - AddRequestHeader=key, value
      routes:
        - id: test_route
          uri: lb://test-service
          predicates:
            -Path=/test/**
```

****

### 2.2 自定义过滤器

#### 1. 自定义 GlobalFilter

```java

@Component
public class MyGlobalFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取请求
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        System.out.println("headers: " + headers);
        // 放行
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 过滤器执行顺序，值越小，优先级越高
        return 0;
    }
}
```

- ServerWebExchange：请求上下文，包含整个过滤器，例如 request、response
- GatewayFilterChain：过滤器链，当前过滤器执行完后，要调用过滤器链中的下一个过滤器

****

#### 2. 自定义 GatewayFilter

自定义 GatewayFilter 不是直接实现 GatewayFilter，而是实现 AbstractGatewayFilterFactory，且该类的名称一定要以
GatewayFilterFactory 为后缀：

```java

@Component
public class PrintAnyGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {
    @Override
    public GatewayFilter apply(Object config) {
        /*return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                System.out.println(exchange.getRequest().getURI());
                return chain.filter(exchange);
            }
        };*/
        // OrderedGatewayFilter 是 GatewayFilter 的子类，包含两个参数：
        // - GatewayFilter：过滤器
        // - int order 值：值越小，过滤器执行优先级越高
        return new OrderedGatewayFilter(new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                System.out.println("PrintAny 过滤器执行了");
                return chain.filter(exchange);
            }
        }, 1);
    }
}
```

可以直接返回一个 GatewayFilter 过滤器内部类，也可以使用 OrderedGatewayFilter 指定优先级（因为内部类不能实现接口），然后在
yaml 配置中这样使用：

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - PrintAny # 此处直接以自定义的GatewayFilterFactory类名称前缀类声明过滤器
```

另外，这种过滤器还可以支持动态配置参数，不过实现起来比较复杂：

```java

@Component
public class PrintAnyGatewayFilterFactory extends AbstractGatewayFilterFactory<PrintAnyGatewayFilterFactory.Config> {
    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter(new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                // 获取 config 值
                String a = config.getA();
                String b = config.getB();
                String c = config.getC();
                // 编写过滤器逻辑
                System.out.println("a = " + a); // 1
                System.out.println("b = " + b); // 2
                System.out.println("c = " + c); // 3
                // 放行
                return chain.filter(exchange);
            }
        }, 100);
    }

    // 自定义配置属性，成员变量名称很重要，下面会用到
    @Data
    static class Config {
        private String a;
        private String b;
        private String c;
    }

    // 将变量名称依次返回，顺序很重要，将来读取参数时需要按顺序获取
    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("a", "b", "c");
    }

    // 返回当前配置类的类型，也就是内部的 Config
    @Override
    public Class<Config> getConfigClass() {
        return Config.class;
    }
}
```

然后在 yaml 配置中这样使用：

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - PrintAny=1,2,3 # 注意，这里多个参数以 "," 隔开，将来会按照 shortcutFieldOrder() 方法返回的参数顺序依次复制
```

上面这种配置方式参数必须严格按照 shortcutFieldOrder() 方法的返回参数名顺序来赋值，还有一种用法，无需按照这个顺序，就是手动指定参数名：

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: PrintAny
          args: # 手动指定参数名，无需按照参数顺序
            a: 1
            b: 2
            c: 3
```

****

### 2.3 登录校验

首先需要获取到 request，然后通过 request 获取到请求路径，通过和配置文件中设置的路径对比，看该路径是否为无需拦截的路径：

```java
if(isExclude(request.getPath().

toString())){
        // 无需拦截，直接放行
        return chain.

filter(exchange);
}

private boolean isExclude(String antPath) {
    for (String pathPattern : authProperties.getExcludePaths()) {
        if (antPathMatcher.match(pathPattern, antPath)) {
            return true;
        }
    }
    return false;
}
```

使用 Spring 提供的路径匹配工具，让获取的请求路径和自定义的排除路径做比较，返回 true 则无需拦截：

```yaml
hm:
  auth:
    excludePaths: # 无需登录校验的路径
      - /search/**
      - /users/login
      - /items/**
```

```java

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthProperties.class)
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtTool jwtTool;

    private final AuthProperties authProperties;

    // Spring 提供的路径匹配工具，支持 **、* 等通配符，用于判断请求是否匹配白名单路径
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 获取 Request
        ServerHttpRequest request = exchange.getRequest();
        // 2. 判断是否不需要拦截
        if (isExclude(request.getPath().toString())) {
            // 无需拦截，直接放行
            return chain.filter(exchange);
        }
        // 3. 获取请求头中的 token
        String token = null;
        List<String> headers = request.getHeaders().get("authorization");
        if (!CollUtils.isEmpty(headers)) {
            token = headers.get(0);
        }
        // 4. 校验并解析token
        Long userId = null;
        try {
            userId = jwtTool.parseToken(token);
        } catch (UnauthorizedException e) {
            // 如果无效，拦截
            ServerHttpResponse response = exchange.getResponse();
            response.setRawStatusCode(401);
            return response.setComplete();
        }

        // TODO 5. 如果有效，传递用户信息
        System.out.println("userId = " + userId);
        // 6. 放行
        return chain.filter(exchange);
    }

    private boolean isExclude(String antPath) {
        for (String pathPattern : authProperties.getExcludePaths()) {
            if (antPathMatcher.match(pathPattern, antPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
```

****

### 2.4 微服务获取用户

由于网关发送请求到微服务依然采用的是 Http 请求，所以可以将用户信息以请求头的方式传递到下游微服务，然后微服务就可以从请求头中获取登录用户信息。
考虑到微服务内部可能很多地方都需要用到登录用户信息，因此可以利用 SpringMVC 的拦截器来实现登录用户信息获取，并存入
ThreadLocal，方便后续使用。

#### 1. 保存用户到请求头

ServerWebExchange 是 WebFlux 中的请求上下文对象，包含了请求 ServerHttpRequest 和响应 ServerHttpResponse，它是不可变的，如果要修改
request，
比如添加请求头，就得调用 exchange.mutate() 来创建一个新的、可修改的构建器。然后放行时不再使用 exchange 上下文对象，而是使用修改了
request 的上下文对象。
而 builder 是 ServerHttpRequest.Builder 对象，它是用来构建修改后的 ServerHttpRequest。`.header("user-info", userInfo)`
表示在原始请求的基础上，
添加一个名为 user-info 的请求头，值是用户 ID（转成字符串）。需要注意的是：header() 方法是追加，不会覆盖已有的同名
header（如果存在多个值，会变成列表）

```java
// 5. 如果有效，传递用户信息
System.out.println("userId = "+userId);

String userInfo = userId.toString();
ServerWebExchange swe = exchange.mutate()
        .request(builder -> builder.header("user-info", userInfo))
        .build();
// 6. 放行
return chain.

filter(swe);
```

在微服务架构中，一般只有网关会直接接触到客户端发来的 JWT token。下游服务（如 user-service, trade-service）通常不再自己解析
token，而是依赖网关，
网关通过请求头 header（如 "user-info"）传递给下游服务。

****

#### 2. 拦截器获取用户

由于每个微服务都有获取登录用户的需求，因此拦截器可以直接写在 hm-common 中，并写好自动装配，这样微服务只需要引入 hm-common
就可以直接具备拦截器功能，无需重复编写。

```java
public class UserInfoInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取请求头中的用户信息
        String userInfo = request.getHeader("user-info");
        // 2. 判断是否为空
        if (StrUtil.isNotBlank(userInfo)) {
            // 不为空，保存到 ThreadLocal
            UserContext.setUser(Long.valueOf(userInfo));
        }
        // 3.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserContext.removeUser();
    }
}
```

然后在 hm-common 模块下编写 SpringMVC 的配置类，配置登录拦截器：

```java

@Configuration
@ConditionalOnClass(DispatcherServlet.class)
public class MvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserInfoInterceptor());
    }
}
```

需要注意的是：这个配置类默认是不会生效的，因为它所在的包是 com.hmall.common.config，与其它微服务的扫描包不一致，无法被扫描到，因此无法生效。但基于
SpringBoot 的自动装配原理，
只要将其添加到 resources 目录下的 META-INF/spring.factories 文件中即可被扫描到：

```factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.hmall.common.config.MyBatisConfig,\
  com.hmall.common.config.MvcConfig,\
  com.hmall.common.config.JsonConfig
```

即告诉 Spring Boot：当项目引入了这个模块（hm-common）时，请自动加载 MvcConfig 这个类中的配置。但在 Springboot 3.x
版本后就不再使用这种方式了，
而是使用 org.springframework.boot.autoconfigure.AutoConfiguration.imports，即在 resources 目录下的 META-INF/spring/ 创建
org.springframework.boot.autoconfigure.AutoConfiguration.imports 文件，
然后在文件中添加：

```text
com.hmall.common.config.MyBatisConfig
com.hmall.common.config.MvcConfig
com.hmall.common.config.JsonConfig
```

还有一点需要注意：Spring Cloud Gateway 是基于 Spring WebFlux 的响应式编程模型构建的，而 Spring MVC 是基于 Servlet API
的阻塞式编程模型，
两者不能同时存在于同一个应用程序中。而配置的拦截器 WebMvcConfig 是属于 Spring MVC 的，但网关模块中也引入了该包，所以启动时必定会报错，
所以需要使用到一个注解：@ConditionalOnClass，它的作用就是当条件生效时该类才加载，所以可以使用 @ConditionalOnClass(
DispatcherServlet.class)，
因为微服务使用的是 SpringMVC，那就一定有这个转发请求的类存在，而网关中一定没有，所以网关模块中就不会加载 SpringMVC
的配置，从而避免发生报错。

****

### 2.5 OpenFeign 传递用户

前端发起的请求都会经过网关再到微服务，搭配过滤器和拦截器微服务可以获取登录用户信息。但是有些业务会在微服务之间调用其它微服务，也就是说这些方法的调用不会经过网关，
那么也就无法获取到存放在请求头中的 userInfo。例如：下单的过程中，需要调用商品服务扣减库存，即调用购物车服务清理用户购物车，而清理购物车时必须知道当前登录的用户身份。
但是，订单服务调用购物车时并没有传递用户信息，购物车服务无法知道当前用户是谁，即 SQL 中的 where userId = ? 为
null，执行肯定失败。而微服务之间调用是基于 OpenFeign 来实现的，
并不是手动发送的请求，所以要借助 Feign 中提供的一个拦截器接口：feign.RequestInterceptor：

```java
public interface RequestInterceptor {
    /**
     * Called for every request. 
     * Add data using methods on the supplied {@link RequestTemplate}.
     */
    void apply(RequestTemplate template);
}
```

只需要实现这个接口并重写 apply 方法，利用 RequestTemplate 类来添加请求头，将用户信息保存到请求头中，每次 OpenFeign
发起请求的时候都会调用该方法，传递用户信息。
由于 FeignClient 全部都是在 hm-api 模块，所以直接在 hm-api 模块的 com.hmall.api.config.DefaultFeignConfig 中编写这个拦截器：

```java

@Bean
public RequestInterceptor userInfoRequestInterceptor() {
    return new RequestInterceptor() {
        @Override
        public void apply(RequestTemplate template) {
            // 获取登录用户
            Long userId = UserContext.getUser();
            if (userId == null) {
                // 如果为空则直接跳过
                return;
            }
            // 如果不为空则放入请求头中，传递给下游微服务
            template.header("user-info", userId.toString());
        }
    };
}
```

RequestTemplate 就是用于组装请求信息的工具，这个 template.header(...) 就是给 OpenFeign
的这次请求添加一个请求头，底层会在实际发送请求时将添加的所有信息变成真实的 HTTP 请求。
因为在 Controller 请求入口时，通过 Spring 拦截器就提取请求头中的用户信息，也就是一经过网关，用户信息就被读出并存在
ThreadLocal 了，在调用 Feign 请求前，
就从 ThreadLocal 中拿出用户信息，主动添加到请求头中，转发给下一个微服务。

****

## 3. 配置管理

可以把微服务共享的配置抽取到 Nacos 中统一管理，这样就不需要每个微服务都重复配置了，分为两步：

- 在 Nacos 中添加共享配置
- 微服务拉取配置

### 3.1 配置共享

以 cart-service 为例，看有哪些配置是重复的，可以抽取的：

```yaml
server:
  port: 8082
spring:
  application:
    name: cart-service # 微服务名称
  cloud:
    nacos:
      server-addr: 127.0.0.1:8848 # nacos地址
  profiles:
    active: dev
  datasource:
    url: jdbc:mysql://${hm.db.host}:3306/hm-cart?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: ${hm.db.pw}
mybatis-plus:
  configuration:
    default-enum-type-handler: com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler
  global-config:
    db-config:
      update-strategy: not_null
      id-type: auto
logging:
  level:
    com.hmall: debug
  pattern:
    dateformat: HH:mm:ss:SSS
  file:
    path: "logs/${spring.application.name}"
knife4j:
  enable: true
  openapi:
    title: 黑马商城购物车管理接口文档
    description: "黑马商城购物车管理接口文档"
    email: zhanghuyi@itcast.cn
    concat: 虎哥
    url: https://www.itcast.cn
    version: v1.0.0
    group:
      default:
        group-name: default
        api-rule: package
        api-rule-resources:
          - com.hmall.cart.controller
```

例如 jdbc 相关配置，这些配置在微服务中基本是一致的，只有某些需要动态的修改，例如数据库的名字，端口地址等：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${hm.db.host}:3306/hm-cart?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: ${hm.db.pw}
```

```yaml
mybatis-plus:
  configuration:
    default-enum-type-handler: com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler
  global-config:
    db-config:
      update-strategy: not_null
      id-type: auto
```

在 Nacos 控制台中进入配置列表，点击 "+" 号，Data id 填写：shared-jdbc.yaml，配置内容填写：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${hm.db.host:127.0.0.1}:${hm.db.port:3306}/${hm.db.database}?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${hm.db.un:root}
    password: ${hm.db.pw:123}
mybatis-plus:
  configuration:
    default-enum-type-handler: com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler
  global-config:
    db-config:
      update-strategy: not_null
      id-type: auto
```

- 数据库 ip：通过 ${hm.db.host:127.0.0.1} 配置了默认值为 127.0.0.1，同时允许通过 ${hm.db.host} 来覆盖默认值（从 SpringBoot
  的 yaml 文件中获取）
- 数据库端口：通过 ${hm.db.port:3306} 配置了默认值为 3306，同时允许通过 ${hm.db.port} 来覆盖默认值
- 数据库 database：可以通过 ${hm.db.database} 来设定，无默认值

```yaml
logging:
  level:
    com.hmall: debug
  pattern:
    dateformat: HH:mm:ss:SSS
  file:
    path: "logs/${spring.application.name}"
```

```yaml
knife4j:
  enable: true
  openapi:
    title: ${hm.swagger.title:黑马商城接口文档}
    description: ${hm.swagger.description:黑马商城接口文档}
    email: ${hm.swagger.email:zhanghuyi@itcast.cn}
    concat: ${hm.swagger.concat:虎哥}
    url: https://www.itcast.cn
    version: v1.0.0
    group:
      default:
        group-name: default
        api-rule: package
        api-rule-resources:
          - ${hm.swagger.package}
```

- title：接口文档标题，用 ${hm.swagger.title} 来代替，将来可以有用户手动指定
- email：联系人邮箱，用 ${hm.swagger.email:zhanghuyi@itcast.cn}，默认值是 zhanghuyi@itcast.cn，同时允许利用 $
  {hm.swagger.email} 来覆盖

要在微服务拉取共享配置，就需要将拉取到的共享配置与本地的 application.yaml 配置合并，完成项目上下文的初始化。但需要注意的是，
读取 Nacos 配置是 SpringCloud 上下文（ApplicationContext）初始化时处理的，发生在项目的引导阶段。然后才会初始化 SpringBoot
上下文，去读取 application.yaml。
也就是说在引导阶段 application.yaml 文件还没有被读取，也就无法加载 Nacaos 的地址并完成配置的合并，
所以 SpringCloud 在初始化上下文的时候会先读取一个名为 bootstrap.yaml（或 bootstrap.properties）的文件，所以可以将 nacos
地址配置到 bootstrap.yaml 中，
那么在项目引导阶段就可以读取 nacos 中的配置了。微服务整合 Nacos 配置管理的步骤如下：

1、引入依赖：

```xml
<!--nacos配置管理-->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
        <!--读取bootstrap文件-->
<dependency>
<groupId>org.springframework.cloud</groupId>
<artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
```

2、resources 目录新建 bootstrap.yaml

```yaml
spring:
  application:
    name: cart-service # 服务名称
  profiles:
    active: dev
  cloud:
    nacos:
      server-addr: 127.0.0.1 # nacos地址
      config:
        file-extension: yaml # 文件后缀名
        shared-configs: # 共享配置
          - dataId: shared-jdbc.yaml # 共享mybatis配置
          - dataId: shared-log.yaml # 共享日志配置
          - dataId: shared-swagger.yaml # 共享日志配置
```

3、修改 application.yaml

```yaml
# 移除已经写在共享配置中的内容，并补充需要动态填写的内容
server:
  port: 8082

hm:
  swagger:
    title: 购物车服务接口文档
    package: com.hmall.cart.controller
  db:
    database: hm-cart
```

日志输出：

```text
Ignore the empty nacos configuration and get it based on dataId[cart-service] & group[DEFAULT_GROUP]
Ignore the empty nacos configuration and get it based on dataId[cart-service.yaml] & group[DEFAULT_GROUP]
Ignore the empty nacos configuration and get it based on dataId[cart-service-local.yaml] & group[DEFAULT_GROUP]
```

****

### 3.2 配置热更新

有很多的业务相关参数，将来可能会根据实际情况临时调整。例如购物车业务，购物车数量有一个上限，默认是 10，代码如下：

```java
private void checkCartsFull(Long userId) {
    Long count = lambdaQuery().eq(Cart::getUserId, userId).count();
    if (count >= 10) {
        throw new BizIllegalException(StrUtil.format("用户购物车课程不能超过{}", 10));
    }
}
```

现在这里购物车是写死的固定值，应该将其配置在配置文件中，方便后期修改。但现在的问题是，即便写在配置文件中，修改了配置还是需要重新打包、重启服务才能生效。
而 Nacos 提供了热更新能力，可以直接让配置文件生效，无序重启项目。

在 nacos 中添加一个配置文件 cart-service.yaml，将购物车的上限数量添加到配置中：

```yaml
hm:
  cart:
    maxAmount: 1 # 购物车商品数量上限
```

需要注意该热更新文件的文件名格式（dataId）：

```text
[服务名]-[spring.profiles.active].[后缀名]
```

- 服务名：因为是购物车服务，所以是 cart-service
- spring.active.profile：就是 SpringBoot 中的 spring.profiles.active，可以省略，则所有 profile 共享该配置

在 Spring Boot 中，激活某个配置环境（Profile）应该写成：

```yaml
spring:
  profiles:
    active: dev
```

例如：cart-service-dev.yaml，代表指定激活 dev 的环境，所有的 Nacos 配置文件包括共享的，都是这样的命名规则，SpringBoot 会根据
active 选择对应的开发环境。

- 后缀名：例如 yaml

在 cart-service 中新建一个属性读取类：

```java

@Data
@Component
@ConfigurationProperties(prefix = "hm.cart")
public class CartProperties {
    private Integer maxAmount;
}
```

****

### 3.3 动态路由

目前网关的路由配置全部是在项目启动时加载的，并且一经加载就会缓存到内存中的路由表内，如果新增或修改了路径，就需要重启服务，如果需要实时更新的话，就可以利用
Nacos 的热更新技术，
手动把路由更新到路由表中。在 Nacos 官网中给出了手动监听 Nacos 配置变更的
SDK：[https://nacos.io/zh-cn/docs/sdk.html](https://nacos.io/zh-cn/docs/sdk.html)。
如果希望 Nacos 推送配置变更，可以使用 Nacos 动态监听配置接口来实现：

```java
public void addListener(String dataId, String group, Listener listener)
```

- dataId：配置 ID，保证全局唯一性，只允许英文字符和 4 种特殊字符（"."、":"、"-"、"_"），不超过 256 字节
- group：配置分组，一般是默认的 DEFAULT_GROUP
- listener：监听器，配置变更进入监听器的回调函数，并将更新后的配置推送给服务端

示例代码：

```java
String serverAddr = "{serverAddr}";
String dataId = "{dataId}";
String group = "{group}";
// 1. 创建 ConfigService，连接 Nacos
Properties properties = new Properties();
properties.

put("serverAddr",serverAddr);

ConfigService configService = NacosFactory.createConfigService(properties);
// 2. 读取配置
String content = configService.getConfig(dataId, group, 5000);
// 3. 添加配置监听器
configService.

addListener(dataId, group, new Listener() {
    @Override
    public void receiveConfigInfo (String configInfo){
        // 配置变更的通知处理
        System.out.println("recieve1:" + configInfo);
    }
    @Override
    public Executor getExecutor () {
        return null;
    }
});
```

这里核心的步骤有 2 步：

- 创建 ConfigService，目的是连接到 Nacos
- 添加配置监听器，编写配置变更的通知处理逻辑

在官方代码中，通过获取到 configService 来连接 nacos，但因为后端采用了 spring-cloud-starter-alibaba-nacos-config 自动装配，
因此 ConfigService 已经在 com.alibaba.cloud.nacos.NacosConfigAutoConfiguration 中自动创建好了：

```java

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = {"spring.cloud.nacos.config.enabled"}, matchIfMissing = true)
public class NacosConfigAutoConfiguration {
    @Bean
    public NacosConfigManager nacosConfigManager(NacosConfigProperties nacosConfigProperties) {
        return new NacosConfigManager(nacosConfigProperties);
    }
}
```

因为 NacosConfigManager 是一个 Bean，所以它可以自动注入，并且 NacosConfigManager 是负责管理 Nacos 的 ConfigService
的，因此，只要拿到 NacosConfigManager 就等于拿到了 ConfigService，即可以连接到 Nacos 了。具体代码如下：

```java
// 配置管理器
public class NacosConfigManager {
  ...

    public NacosConfigManager(NacosConfigProperties nacosConfigProperties) {
        this.nacosConfigProperties = nacosConfigProperties;
        createConfigService(nacosConfigProperties); // 创建 configService
    }

    static ConfigService createConfigService(NacosConfigProperties nacosConfigProperties) {
    ...
        service = NacosFactory.createConfigService(nacosConfigProperties.assembleConfigServiceProperties());
    ...
        return service;
    }

    public ConfigService getConfigService() { // 读取 configService
        if (Objects.isNull(service)) {
            createConfigService(this.nacosConfigProperties);
        }
        return service;
    }
  ...
}
```

第二步，编写监听器。虽然官方提供的 SDK 是 ConfigService 中的 addListener，不过项目第一次启动时不仅仅需要添加监听器，也需要读取配置，因此建议使用的
API 是这个：

```java
// ConfigService 接口中的方法
String getConfigAndSignListener(String var1, String var2, long var3, Listener var5) throws NacosException;

// 实现类中的配置
String getConfigAndSignListener(
        String dataId, // 配置文件id
        String group, // 配置组，走默认
        long timeoutMs, // 读取配置的超时时间
        Listener listener // 监听器
) throws NacosException;
```

该方法既可以配置监听器，还会根据 dataId 和 group 读取配置并返回，然后就可以在项目启动时先更新一次路由，后续随着配置变更通知到监听器，完成路由更新。在
Spring Cloud Gateway 中，
所有的路由定义最终都要写入到 RouteDefinitionRouteLocator 维护的内存路由表中，而写入这张表的接口就是
RouteDefinitionWriter，它提供了两个功能：

```java
public interface RouteDefinitionWriter {
    // 更新路由到路由表，如果路由id重复，则会覆盖旧的路由
    Mono<Void> save(Mono<RouteDefinition> route);

    // 根据路由id删除某个路由
    Mono<Void> delete(Mono<String> routeId);
}
```

因为 Spring Cloud Gateway 默认是基于配置文件静态加载路由表，而现在是要从 Nacos 配置中心动态获取路由表然后再写入内存中，所以需要手动调用该接口的两个方法。而保存到
Nacos 的配置则使用 json 的格式，
因为动态路由机制是代码主动拉取并解析配置，而 RouteDefinitionWriter 是基于 JSON 对象模型进行解析的，它不能像 Spring Boot
一样自动解析 yaml 格式的数据。例如：

```json
{
  "id": "item",
  "predicates": [
    {
      "name": "Path",
      "args": {
        "_genkey_0": "/items/**",
        "_genkey_1": "/search/**"
      }
    }
  ],
  "filters": [],
  "uri": "lb://item-service"
}
```

具体使用：

1、引入依赖

```xml
<!--统一配置管理-->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
        <!--加载bootstrap-->
<dependency>
<groupId>org.springframework.cloud</groupId>
<artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
```

2、创建 bootstrap.yaml 文件

```yaml
spring:
  application:
    name: gateway # 服务名称
  profiles:
    active: dev
  cloud:
    nacos:
      server-addr: 127.0.0.1 # nacos地址
      config:
        file-extension: yaml # 文件后缀名
        shared-configs: # 共享配置
          - dataId: shared-log.yaml # 共享日志配置
```

3、编写代码

因为把 DynamicRouteLoader 写成了一个配置类，所以它在 SpringBoot 加载时就会被初始化，而 initRouteConfigListener() 被添加了
@PostConstruct 注解（Bean 初始化后会自动执行），
所以它会在 Bean 初始化后就注册监听并拉取 Nacos 中的配置文件，然后通过 RouteDefinitionWriter
更新与删除路由表。需要注意的是：RouteDefinitionWriter 没有提供更新操作，只有新增操作，
所以如果要保证整体路由表的实时性，需要在每次 Nacos 更新数据时，这里执行删除所有内存中路由的操作，然后再把 Nacos
中的所有路由添加进内存。

```java

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouteLoader {

    private final RouteDefinitionWriter writer;
    private final NacosConfigManager nacosConfigManager;

    // 路由配置文件的id和分组
    private final String dataId = "gateway-routes.json";
    private final String group = "DEFAULT_GROUP";
    // 保存更新过的路由 id
    private final Set<String> routeIds = new HashSet<>();

    // Bean 初始化后再执行
    @PostConstruct
    public void initRouteConfigListener() throws NacosException {
        // 1. 注册监听器并首次拉取配置
        String configInfo = nacosConfigManager.getConfigService()
                .getConfigAndSignListener(dataId, group, 5000, new Listener() {
                    @Override
                    public Executor getExecutor() { // 定义线程池
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        // 监听到配置变更，需要更新路由表
                        updateConfigInfo(configInfo);
                    }
                });
        // 2. 首次启动时，更新一次配置
        updateConfigInfo(configInfo);
    }

    private void updateConfigInfo(String configInfo) {
        log.debug("监听到路由配置变更：{}", configInfo);
        // 1. 反序列化，将 json 文件转换成 RouteDefinition
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(configInfo, RouteDefinition.class);
        // 2. 更新前先清空旧路由
        for (String routeId : routeIds) {
            writer.delete(Mono.just(routeId)).subscribe();
        }
        // 清空集合
        routeIds.clear();
        // 判断是否有新的路由要更新
        if (CollUtils.isEmpty(routeDefinitions)) {
            // 无新路由配置，直接结束
            return;
        }
        // 3. 更新路由
        routeDefinitions.forEach(routeDefinition -> {
            writer.save(Mono.just(routeDefinition)).subscribe();
            // 记录路由 id，方便将来删除
            routeIds.add(routeDefinition.getId());
        });
    }
}
```

监听结果：

```json
// 监听到路由配置变更：
[
  {
    "id": "item",
    "predicates": [
      {
        "name": "Path",
        "args": {
          "_genkey_0": "/items/**",
          "_genkey_1": "/search/**"
        }
      }
    ],
    "filters": [],
    "uri": "lb://item-service"
  },
  ...
]
```

****

# 五、微服务保护和分布式事务

## 1. 微服务保护

### 1.1 雪崩问题

例如在之前的查询购物车列表业务中，购物车服务需要查询最新的商品信息并与购物车数据做对比，提醒用户。可如果商品服务查询时发生故障，查询购物车列表在调用商品服务时，也会异常从而导致购物车查询失败。
但从业务角度来说，为了提升用户体验，即便是商品查询失败，购物车列表也应该正确展示出来，哪怕是不包含最新的商品信息。

还是查询购物车的业务，假如商品服务业务并发较高，占用过多 Tomcat 连接。可能会导致商品服务的所有接口响应时间增加，延迟变高，甚至是长时间阻塞直至查询失败。
此时查询购物车业务需要查询并等待商品查询结果，从而导致查询购物车列表业务的响应时间也变长，甚至也阻塞直至无法访问。而整个微服务中，都可能存在类似的问题，最终导致整个集群不可用。

****

### 1.2 服务保护方案

1、请求限流

服务故障最重要原因，就是并发太高。解决了这个问题，就能避免大部分故障。当然，通常情况下接口的并发不是一直很高，而是突发的，因此请求限流，就是限制或控制接口访问的并发流量，
避免服务因流量激增而出现故障。请求限流往往会有一个限流器，数量高低起伏的并发请求曲线，经过限流器就变的非常平稳。这就像是水电站的大坝，起到蓄水的作用，
可以通过开关控制水流出的大小，让下游水流始终维持在一个平稳的量。

2、线程隔离

当一个业务接口响应时间长，而且并发高时，就可能耗尽服务器的线程资源，导致服务内的其它接口受到影响，所以必须把这种影响降低，或者缩减这种影响的范围。而县城隔离就是为了避免某个接口故障或压力过大导致整个服务不可用，
限定每个接口的可以使用的资源范围，将不同的接口、不同的逻辑，用不同的线程池处理，防止相互影响。

3、服务熔断

线程隔离虽然避免了雪崩问题，但故障服务（商品服务）依然会拖慢购物车服务（服务调用方）的接口响应速度。而且商品查询的故障依然会导致查询购物车功能出现故障，购物车业务也变的不可用了。
所以需要：

- 编写服务降级逻辑：就是服务调用失败后的处理逻辑，根据业务场景，可以抛出异常，也可以返回提示或默认数据
- 异常统计和熔断：统计服务提供方的异常比例，当比例过高表明该接口会影响到其它服务，应该拒绝调用该接口，并直接走降级逻辑

****

## 2. Sentinel

### 2.1 安装

Sentinel 是阿里巴巴开源的一款面向分布式服务架构的流量控制组件，专注于流量控制、熔断降级、系统负载保护等。在微服务架构中，一个接口的异常不应影响整个系统，
而 Sentinel 就是用来保证系统稳定性和可用性的关键中间件。

Sentinel 的使用可以分为两个部分:

- 核心库（Jar包）：不依赖任何框架/库，能够运行于 Java 8 及以上的版本的运行时环境，同时对 Dubbo / Spring Cloud
  等框架也有较好的支持。在项目中引入依赖即可实现服务限流、隔离、熔断等功能。
- 控制台（Dashboard）：Dashboard 主要负责管理推送规则、监控、管理机器信息等。

具体使用步骤：

1、下载 jar 包

下载地址：[https://github.com/alibaba/Sentinel/releases](https://github.com/alibaba/Sentinel/releases)

2、运行

将jar包放在任意非中文、不包含特殊字符的目录下，重命名为sentinel-dashboard.jar，然后在 cmd 中运行如下命令：

```shell
java "-Dserver.port=8099" "-Dcsp.sentinel.dashboard.server=localhost:8099" "-Dproject.name=sentinel-dashboard" -jar sentinel-dashboard.jar
```

3、访问 http://localhost:8099 页面

登录需要输入账号和密码，默认都是：sentinel

在 cart-service 模块中整合 sentinel，连接 sentinel-dashboard 控制台，步骤如下：

1、引入 sentinel 依赖

```xml
<!--sentinel-->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

2、配置控制台

修改 application.yaml 文件，添加下面内容：

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8099
```

3、访问 cart-service 的任意端点

重启 cart-service，然后访问查询购物车接口，sentinel 的客户端就会将服务访问的信息提交到 sentinel-dashboard 控制台，并展示出统计信息。

点击簇点链路可以看到类似 controller 层的请求路径。所谓簇点链路，就是单机调用链路，是一次请求进入服务后经过的每一个被
Sentinel 监控的资源。默认情况下，
Sentinel 会监 控SpringMVC 的每一个 Endpoint（即 Http 接口）。所以看到的 /carts 接口路径就是其中一个簇点，可以对其进行限流、熔断、隔离等保护措施。
不过，需要注意的是，该项目的 SpringMVC 接口是按照 Restful 风格设计，因此购物车的查询、删除、修改等接口全部都是 /carts
路径。而默认情况下，Sentinel 会把路径作为簇点资源的名称，
无法区分路径相同但请求方式不同的接口，而查询、删除、修改等都被识别为一个簇点资源，这显然是不合适的。所以可以选择打开 Sentinel
的请求方式前缀，把请求方式 + 请求路径作为簇点资源名。
在 cart-service 的 application.yml 中添加下面的配置：

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8090
      http-method-specify: true # 开启请求方式前缀
```

****

### 2.2 请求限流

在簇点链路后面点击流控按钮，即可对其做限流配置，在弹出的菜单中的阈值类型选择 QPS，单机阈值填写 6，这样就把查询购物车列表这个簇点资源的流量限制在了每秒
6 个，也就是最大 QPS 为 6。
利用 JMeter 进行测试，开启 1000 个线程，运行时间为 100 s，所以大概是 1 s 运行 10 次，而 GET:/carts 这个接口的通过 QPS 稳定在
6 附近，而拒绝的 QPS 在 4 附近。

****

### 2.3 线程隔离

限流可以降低服务器压力，尽量减少因并发流量引起的服务故障的概率，但并不能完全避免服务故障，一旦某个服务出现故障，就必须隔离对这个服务的调用，避免发生雪崩。比如，查询购物车的时候需要查询商品，
为了避免因商品服务出现故障导致购物车服务级联失败，可以把购物车业务中查询商品的部分隔离起来，限制可用的线程资源。而查询商品又调用了别的微服务的功能，它不涉及
Http 请求，
所以要通过 OpenFeign 整合 Sentinel。

修改 cart-service 模块的 application.yml 文件，开启 Feign 的 sentinel 功能：

```yaml
feign:
  sentinel:
    enabled: true # 开启feign对sentinel的支持
```

需要注意的是，默认情况下 SpringBoot 项目的 tomcat 最大线程数是 200，允许的最大连接是 8492，单机测试很难打满。所以需要配置一下
cart-service 模块的 application.yml 文件，
修改 tomcat 连接：

```yaml
server:
  port: 8082
  tomcat:
    threads:
      max: 50 # 允许的最大线程数
    accept-count: 50 # 最大排队等待数量
    max-connections: 100 # 允许的最大连接
```

在 sentinel 页面可以看到查询商品的 FeignClient 自动变成了一个簇点资源：

```text
GET:/carts
    GET:http://item-service/items // 查询商品，是查询购物车的下级链路
```

需要注意的是：这里使用的 spring-cloud-alibaba 依赖不能使用 2022.x 版本的了，不然会报错，得换用 2023 版本：

```xml

<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-alibaba-dependencies</artifactId>
    <version>2023.0.1.0</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

点击查询商品的 FeignClient 对应的簇点资源后面的流控按钮，选择并发线程数，单机阈值填写 5，注意，这里勾选的是并发线程数限制，也就是说这个查询功能最多使用
5 个线程，
而不是 5QPS。一个线程对应 2 个 QPS 如果查询商品的接口每秒处理 2 个请求，则 5 个线程的实际 QPS 在 10 左右，而超出的请求自然会被拒绝。利用
Jemeter 测试，创建 5000 个线程，
运行时间 50 s，即每秒发送 100 个请求，最终测试结果：进入查询购物车的请求每秒大概在 100，但基本都拒绝九十多个请求，而在查询商品时却只剩下每秒
10 左右，符合预期。
此时如果通过页面访问购物车的其它接口，例如添加购物车、修改购物车商品数量，发现不受影响，响应时间非常短，这就证明线程隔离起到了作用，尽管查询购物车这个接口并发很高，
但是它能使用的线程资源被限制了，因此不会影响到其它接口。

****

### 2.4 服务熔断

上面利用线程隔离对查询购物车业务进行隔离，保护了购物车服务的其它接口，但由于查询商品的功能耗时较高（模拟了 500
毫秒延时），再加上线程隔离限定了线程数为5，导致接口吞吐能力有限，
最终 QPS 只有 10 左右。这就导致了几个问题：

1、超出的 QPS 上限的请求就只能抛出异常，从而导致购物车的查询失败。但从业务角度来说，即便没有查询到最新的商品信息，购物车也应该展示给用户，用户体验更好。
也就是应该给查询失败设置一个降级处理逻辑。

2、由于查询商品的延迟较高（模拟的 500ms），从而导致查询购物车的响应时间也变的很长。这样不仅拖慢了购物车服务，消耗了购物车服务的更多资源，而且用户体验也很差。
对于商品服务这种不太健康的接口，应该直接停止调用，直接走降级逻辑，避免影响到当前服务，也就是将商品查询接口熔断。

#### 1. 编写降级逻辑

触发限流或熔断后的请求不一定要直接报错，也可以返回一些默认数据或者友好提示，用户体验会更好。给 FeignClient 编写失败后的降级逻辑有两种方式：

- 方式一：FallbackClass，无法对远程调用的异常做处理
- 方式二：FallbackFactory，可以对远程调用的异常做处理，一般选择这种方式

1、在 hm-api 模块中给 ItemClient 定义降级处理类，实现 FallbackFactory：

```java

@Slf4j
public class ItemClientFallback implements FallbackFactory<ItemClient> {
    @Override
    public ItemClient create(Throwable cause) {
        return new ItemClient() {
            @Override
            public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
                log.error("远程调用ItemClient#queryItemByIds方法出现异常，参数：{}", ids, cause);
                // 查询购物车允许失败，查询失败，返回空集合
                return CollUtils.emptyList();
            }

            @Override
            public void deductStock(List<OrderDetailDTO> items) {
                // 库存扣减业务需要触发事务回滚，查询失败，抛出异常
                throw new BizIllegalException(cause);
            }
        };
    }
}
```

2、在 hm-api 模块中的 com.hmall.api.config.DefaultFeignConfig 类中将 ItemClientFallback 注册为一个 Bean：

```java

@Bean
public ItemClientFallback itemClientFallback() {
    return new ItemClientFallback();
}
```

3、在 hm-api 模块中的 ItemClient 接口中使用 ItemClientFallbackFactory：

```java

@FeignClient(name = "item-service", configuration = DefaultFeignConfig.class, fallbackFactory = ItemClientFallback.class)
public interface ItemClient {
  ...
}
```

****

#### 2. 服务熔断

查询商品的 RT 较高（模拟的 500ms），从而导致查询购物车的RT也变的很长，这样不仅拖慢了购物车服务，消耗了购物车服务的更多资源，而且用户体验也很差。对于商品服务这种不太健康的接口，
应该停止调用，直接走降级逻辑，避免影响到当前服务,也就是将商品查询接口熔断。当商品服务接口恢复正常后，再允许调用，这其实就是断路器的工作模式。

Sentinel 中的断路器不仅可以统计某个接口的慢请求比例，还可以统计异常请求比例。当这些比例超出阈值时，就会熔断该接口，即拦截访问该接口的一切请求，降级处理；
当该接口恢复正常时，再放行对于该接口的请求。

断路器的工作状态切换有一个状态机来控制，状态机包括三个状态：

- closed：关闭状态，断路器放行所有请求，并开始统计异常比例、慢请求比例。超过阈值则切换到 open 状态
- open：打开状态，服务调用被熔断，访问被熔断服务的请求会被拒绝，快速失败，直接走降级逻辑。Open 状态持续一段时间后会进入
  half-open 状态
- half-open：半开状态，放行一次请求，根据执行结果来判断接下来的操作。
    - 请求成功：则切换到 closed 状态
    - 请求失败：则切换到 open 状态

可以在控制台通过点击簇点后的熔断按钮来配置熔断策略，在弹出的表格中填写：

```text
资源名：GET:http://item-service/items
熔断策略：慢比例调用
最大 RT：200（ms）   比例阈值：0.5
熔断时长：20     最小请求数：5
统计时长：1000
```

- RT 超过 200 毫秒的请求调用就是慢调用
- 统计最近 1000ms 内的最少 5 次请求，如果慢调用比例不低于 0.5，则触发熔断
- 熔断持续时长 20s

观察 Sentinel 的实时监控，在一开始一段时间是允许访问的，后来触发熔断后，查询商品服务的接口通过 QPS 直接为 0，所有请求都被熔断了，而查询购物车的本身并没有受到影响。

****

## 3. 分布式事务

### 3.1 概述

由于订单、购物车、商品分别在三个不同的微服务，而每个微服务都有自己独立的数据库，因此下单过程中就会跨多个数据库完成业务。而每个微服务都会执行自己的本地事务：

- 交易服务：下单事务
- 购物车服务：清理购物车事务
- 库存服务：扣减库存事务

整个业务中，各个本地事务是有关联的。因此每个微服务的本地事务，也可以称为分支事务。多个有关联的分支事务一起就组成了全局事，所以必须保证整个全局事务同时成功或失败。

****
### 3.2 Seata

分布式事务产生的一个重要原因，就是参与事务的多个分支事务互相无感知，不知道彼此的执行状态，因此可以找一个统一的事务协调者，与多个分支事务通信，检测每个分支事务的执行状态，
保证全局事务下的每一个分支事务同时成功或失败即可，大多数的分布式事务框架都是基于这个理论来实现的。Seata 也不例外，在 Seata 的事务管理中有三个重要的角色：

- TC (Transaction Coordinator) - 事务协调者：是 Seata 的服务端，维护全局和分支事务的状态，协调全局事务提交或回滚
- TM (Transaction Manager) - 事务管理器：是发起全局事务的客户端组件，通常集成在发起服务中；定义全局事务的范围、开始全局事务、提交或回滚全局事务，告诉 TC 什么时候开始和结束
- RM (Resource Manager) - 资源管理器：管理分支事务，向 TC 注册分支事务，并汇报其执行结果；接收 TC 的指令决定提交还是回滚本地事务

TM 和 RM 可以理解为 Seata 的客户端部分，引入到参与事务的微服务依赖中即可。将来 TM 和 RM 就会协助微服务，实现本地分支事务与 TC 之间交互，实现事务的提交或回滚。
而 TC 服务则是事务协调中心，是一个独立的微服务，需要单独部署。

****

### 3.3 部署 TC 服务

#### 1. 创建 docker 容器

1、创建网络

```shell
docker network create hm-net
```

2、将 mysql 和 nacos 添加进 hm-net 网络

```shell
docker network connect hm-net mysql
docker network connect hm-net nacos
```

3、为确保持久化的需要，选择基于数据库存储 seata，添加对应的数据库并存入数据

4、进入本地磁盘的 seata 文件所在目录，然后执行如下命令，创建 seata 容器：

```shell
cd /mnt/d/docker_dataMountDirectory
```

```shell
docker run --name seata \
-p 8099:8099 \
-p 7099:7099 \
-e SEATA_IP=192.168.0.105 \
-v ./seata:/seata-server/resources \
--privileged=true \
--network hm-net \
-d \
seataio/seata-server:1.5.2
```

****
#### 2. 微服务集成 seata

1、引入依赖

为了方便各个微服务集成 seata，需要把 seata 配置共享到 nacos，因此 trade-service 模块不仅仅要引入 seata 依赖，还要引入 nacos 依赖:

```xml
<!--统一配置管理-->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
        <!--读取bootstrap文件-->
<dependency>
<groupId>org.springframework.cloud</groupId>
<artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
        <!--seata-->
<dependency>
<groupId>com.alibaba.cloud</groupId>
<artifactId>spring-cloud-starter-alibaba-seata</artifactId>
</dependency>
```

2、在 nacos 上添加一个共享的 seata 配置，命名为 shared-seata.yaml：

```yaml
seata:
  registry: # TC服务注册中心的配置，微服务根据这些信息去注册中心获取tc服务地址
    type: nacos # 注册中心类型 nacos
    nacos:
      server-addr: host.docker.internal:8848 # nacos地址
      namespace: "" # namespace，默认为空
      group: DEFAULT_GROUP # 分组，默认是DEFAULT_GROUP
      application: seata-server # seata服务名称
      username: nacos
      password: nacos
  tx-service-group: hmall # 事务组名称
  service:
    vgroup-mapping: # 事务组与tc集群的映射关系
      hmall: "default"
```

3、为 trade-service 模块添加 bootstrap.yaml 并修改 application.yaml 文件（hm-cart 和 hm-item 同理）：

```yaml
spring:
  application:
    name: trade-service # 服务名称
  profiles:
    active: dev
  cloud:
    nacos:
      server-addr: host.docker.internal # nacos地址
      config:
        file-extension: yaml # 文件后缀名
        shared-configs: # 共享配置
          - dataId: shared-jdbc.yaml # 共享mybatis配置
          - dataId: shared-log.yaml # 共享日志配置
          - dataId: shared-swagger.yaml # 共享日志配置
          - dataId: shared-seata.yaml # 共享seata配置
```

```yaml
server:
  port: 8084
feign:
  sentinel:
    enabled: true # 开启Feign对Sentinel的整合
hm:
  swagger:
    title: 交易服务接口文档
    package: com.hmall.trade.controller
  db:
    database: hm-trade
```

4、将 seata-at.sql 分别文件导入 hm-trade、hm-cart、hm-item 三个数据库中

5、测试

将对应微服务的 @Transactional 注解改为 Seata 提供的 @GlobalTransactional，该注解就是在标记事务的起点，将来 TM 就会基于这个方法判断全局事务范围，初始化全局事务。

****
### 3.4 XA 模式

XA 是一种分布式事务协议，它是一个 两阶段提交（2PC）协议，由两大角色组成：

- Transaction Manager（TM）：全局事务协调者，负责协调多个本地事务的一致提交
- Resource Manager（RM）：资源管理器，通常是数据库、消息队列等，负责执行本地事务

一阶段：

- 事务协调者通知每个事务参与者执行本地事务
- 本地事务执行完成后报告事务执行状态给事务协调者，此时事务不提交，继续持有数据库锁

二阶段：

- 事务协调者基于一阶段的报告来判断下一步操作
- 如果一阶段都成功，则通知所有事务参与者，提交事务
- 如果一阶段任意一个参与者失败，则通知所有事务参与者回滚事务

而 Seata 对原始的 XA 模式做了简单的封装和改造，以适应自己的事务模型：

RM 一阶段的工作：

1. 注册分支事务到 TC
2. 执行分支业务 sql 但不提交
3. 报告执行状态到 TC

TC 二阶段的工作：

1. TC 检测各分支事务执行状态
    1. 如果都成功，通知所有 RM 提交事务
    2. 如果有失败，通知所有 RM 回滚事务

RM 二阶段的工作：

- 接收 TC 指令，提交或回滚事务

优点：

- 事务的强一致性，满足 ACID 原则
- 常用数据库都支持，实现简单，并且没有代码侵入

缺点：

- 因为一阶段需要锁定数据库资源，等待二阶段结束才释放，性能较差
- 依赖关系型数据库实现事务

实现步骤：

1、在配置文件中指定要采用的分布式事务模式，可以在 Nacos 中的共享 shared-seata.yaml 配置文件中设置：

```yaml
seata:
  data-source-proxy-mode: XA
```

2、添加 @GlobalTransactional 标记分布式事务的入口

****

### 3.5 AT 模式

AT 模式同样是分阶段提交的事务模型，不过缺弥补了 XA 模型中资源锁定周期过长的缺陷。

阶段一 RM 的工作：

- 注册分支事务
- 记录 undo-log（数据快照）
- 执行业务 sql 并提交
- 报告事务状态

阶段二提交时 RM 的工作：

- 删除 undo-log

阶段二回滚时 RM 的工作：

- 根据 undo-log 恢复数据到更新前

实现步骤与 XA 模式一致，只需要把 Nacos 中的共享文件中的 XA 修改为 AT，而 AT 是默认的模式，所以可以不写：

```yaml
seata:
# data-source-proxy-mode: AT
```

例如，现在有一个数据库表，记录用户余额，其中一个分支业务要执行的 SQL 为：

```sql
update tb_account
set money = money - 10
where id = 1
```

AT 模式下，当前分支事务执行流程如下：

一阶段：

1. TM 发起并注册全局事务到 TC
2. TM 调用分支事务
3. 分支事务准备执行业务 SQL
4. RM 拦截业务 SQL，根据 where 条件查询原始数据，形成快照

```json
{
  "id": 1,
  "money": 100
}
```

1. RM 执行业务 SQL，提交本地事务，释放数据库锁，此时 money = 90
2. RM 报告本地事务状态给 TC

二阶段：

1. TM 通知 TC 事务结束
2. TC 检查分支事务状态
    1. 如果都成功，则立即删除快照
    2. 如果有分支事务失败，需要回滚。读取快照数据（{"id": 1, "money": 100}），将快照恢复到数据库，此时数据库再次恢复为 100

****

### 3.6 AT 与 XA 的区别

- XA模式一阶段不提交事务，锁定资源；AT模式一阶段直接提交，不锁定资源。
- XA模式依赖数据库机制实现回滚；AT模式利用数据快照实现数据回滚。
- XA模式强一致；AT模式最终一致

****

# 六、MQ

## 1. 概述

### 1.1 同步调用

当前项目是基于 OpenFeign 的，所以它的调用都属于是同步调用，它属于一请求一线程，线程阻塞等待响应。这其中就存在 3 个问题：

1、拓展性差

目前的业务相对简单，但是随着业务规模扩大，后续肯定会新增很多功能，但是基于同步调用的机制，新增的功能可能影响原有的功能，导致现有的代码逻辑每次都会随着功能的迭代而更新，这就违背了开闭原则。

2、性能下降

由于采用了同步调用，调用者需要等待服务提供者执行完返回结果后，才能继续向下执行，也就是说每次远程调用，调用者都是阻塞等待状态。最终整个业务的响应时长就是每次远程调用的执行时长之和。
如果每个微服务的执行时长都是 50ms，则最终整个业务的耗时可能高达上百毫秒，性能很差。

3、级联失败

由于是基于 OpenFeign 来调用交易服务、通知服务。当交易服务、通知服务出现故障时，整个事务都会回滚，交易失败。但涉及到支付功能时，如果通知支付成功的接口发生故障，此时就会回滚所有事务，
但用户可能已经完成支付，这就造成钱扣了而支付记录不存在，这样是不合理的。

****

### 1.2 异步调用

异步调用方式就是基于消息通知的方式，一般包含三个角色：

- 消息发送者：投递消息的人，就是原来的调用方
- 消息 Broker：管理、暂存、转发消息，你可以把它理解成微信服务器
- 消息接收者：接收和处理消息的人，就是原来的服务提供方

在异步调用中，发送者不再直接同步调用接收者的业务接口，而是发送一条消息投递给消息 Broker，然后接收者根据自己的需求从消息 Broker 那里订阅消息。每当发送方发送消息后，
接受者都能获取消息并处理，这样发送消息的人和接收消息的人就完全解耦了。而对于扩展新功能来说，也只需要让原有的功能调用完成后发送消息给 Broker，再让性功能接收 Broker 的消息即可，
而整个流程耗时的只是这三个角色的时间，也就是说不管有多少功能，它们都只耗时发送消息时间+更新数据时间+接收消息时间。

****

### 1.3 技术选型

对于消息 Broker 来说，目前常见的实现方案就是消息队列（MessageQueue），简称为 MQ。目比较常见的MQ实现：

- ActiveMQ
- RabbitMQ
- RocketMQ
- Kafka

RabbitMQ：

由 Rabbit 公司 / 社区维护，采用 Erlang 语言开发，支持多种协议，可用性高，消息延迟达微秒级，消息可靠性高（发送消息后确保消费者至少消费一次），但单机吞吐量表现一般，
适用于对可靠性要求高、对吞吐量要求不是极致的场景，比如金融交易的消息通知等。

ActiveMQ：

属于 Apache 社区，基于 Java 开发，协议支持丰富，不过可用性一般，单机吞吐量差，消息延迟为毫秒级，消息可靠性一般，在一些传统企业应用中可能会有使用，
但逐渐被其他更优的消息队列替代。

RocketMQ：

由阿里开发（后捐赠给 Apache），使用 Java 语言，采用自定义协议，可用性高，单机吞吐量高，消息延迟毫秒级，消息可靠性高，适合大规模分布式系统、高吞吐量的业务场景，
像电商的订单交易、物流信息推送等场景常用。

Kafka：

归属于 Apache 社区，基于 Scala 和 Java 开发，自定义协议，可用性高，单机吞吐量非常高，消息延迟能控制在毫秒以内，不过消息可靠性一般，常用于大数据流式处理、
日志采集等对吞吐量要求极高、对可靠性要求相对没那么严苛的场景，例如实时日志分析系统。

| 对比维度  | RabbitMQ             | ActiveMQ                      | RocketMQ | Kafka      |
|-------|----------------------|-------------------------------|----------|------------|
| 公司/社区 | Rabbit               | Apache                        | 阿里       | Apache     |
| 开发语言  | Erlang               | Java                          | Java     | Scala&Java |
| 协议支持  | AMQP，XMPP，SMTP，STOMP | OpenWire，STOMP，REST，XMPP，AMQP | 自定义协议    | 自定义协议      |
| 可用性   | 高                    | 一般                            | 高        | 高          |
| 单机吞吐量 | 一般（每秒十万）             | 差                             | 高        | 非常高（每秒百万）  |
| 消息延迟  | 微秒级                  | 毫秒级                           | 毫秒级      | 毫秒以内       |
| 消息可靠性 | 高                    | 一般                            | 高        | 一般         | 

****
## 2. RabbitMQ

### 2.1 安装

```shell
docker run \
 -e RABBITMQ_DEFAULT_USER=rabbitmq \
 -e RABBITMQ_DEFAULT_PASS=rabbitmq \
 -v mq-plugins:/plugins \
 --name mq \
 --hostname mq \
 -p 15672:15672 \
 -p 5672:5672 \
 --network hm-net\
 -d \
 rabbitmq:4.1.0-management
```

两个映射的端口：

- 15672：RabbitMQ 提供的管理控制台的端口
- 5672：RabbitMQ 的消息发送处理接口

安装完成后，访问 http://localhost:15672 即可看到管理控制台，首次访问需要登录，默认的用户名和密码在配置文件中已经指定了。

关于 RabbitMQ，它有五个核心角色：

1、生产者（Publisher）

它是发送消息的一方，负责产生消息。比如在电商系统中，订单创建服务就可以作为生产者，当有新订单创建时，它将订单相关消息发送给 RabbitMQ。

2、消费者（Consumer）

它是接收并处理消息的一方，在电商系统中，库存服务可以作为消费者，接收订单创建服务发送的消息，从而更新库存信息。

3、队列（Queue）

队列用于存储消息，生产者发送的消息会暂时存放在队列中，等待消费者来获取和处理。一个队列可以有多个消费者，并且消息一旦被消费，通常会从队列中移除（默认情况，可配置持久化等策略）。
例如在一个日志收集系统中，日志产生的消息会被发送到对应的队列中，由日志处理服务从队列里读取消息然后进行分析处理。

4、交换机（Exchange）

负责消息的路由。生产者将消息发送到交换机，交换机根据特定的路由规则（绑定关系），决定将消息投递到一个或多个队列中。RabbitMQ 支持多种类型的交换机，比如：

- Direct Exchange（直连交换机）：根据消息携带的路由键（Routing Key）进行路由，只有当路由键完全匹配时，消息才会被投递到对应的队列 
- Fanout Exchange（扇形交换机）：会将接收到的消息广播到所有绑定的队列，不考虑路由键
- Topic Exchange（主题交换机）：根据路由键和绑定键的匹配规则进行路由，支持通配符，例如使用 "*" 匹配一个单词，"#" 匹配零个或多个单词
- Headers Exchange（头交换机）：基于消息的头部属性进行路由，但使用相对较少

5、虚拟主机（Virtual Host）

起到数据隔离的作用，相当于一个独立的小型 RabbitMQ 服务器，每个虚拟主机都有自己独立的交换机、队列、绑定关系等，不同虚拟主机之间相互隔离，互不影响。可以用于多租户场景，
或者在开发、测试、生产环境之间进行隔离 。

****
### 2.2 收发消息

#### 1. 交换机

打开 Exchanges 选项卡，可以看到已经存在很多交换机：

| Virtual host | Name                  | Type    | Features | Message rate in | Message rate out |
|--------------|-----------------------|---------|----------|-----------------|------------------|
| /            | (AMQP default)        | direct  | D        |                 |                  |
| /            | amq.direct            | direct  | D        |                 |                  |
| /            | amq.fanout            | fanout  | D        |                 |                  |
| /            | amq.headers           | headers | D        |                 |                  |
| /            | amq.match             | headers | D        |                 |                  |
| /            | amq.rabbitmq.trace    | topic   | D、I      |                 |                  |
| /            | amq.topic             | topic   | D        |                 |                  | 

- **Virtual host**：虚拟主机，用于在 RabbitMQ 中实现资源隔离，这里均为根虚拟主机 `/` 
- **Name**：交换机名称，`(AMQP default)` 是默认直连交换机等，`amq.` 开头的多为 RabbitMQ 内置的交换机
- **Type**：交换机类型，`direct`（直连）、`fanout`（扇形/广播）、`headers`（头匹配）、`topic`（主题），不同类型决定消息路由规则
- **Features**：功能标识，`D` 一般代表可持久化（durable），`I` 可能是与内部（internal）相关特性（如 `amq.rabbitmq.trace` 用于消息追踪相关内部功能）
- **Message rate in**：进入交换机的消息速率，图中暂未显示具体数值
- **Message rate out**：从交换机出去的消息速率，图中也暂未显示具体数值

然后点击任意交换机，就可以进入交换机详情页面，这里会利用控制台中的 publish message 发送一条消息：

```text
┌─────────────────────────────────────────────────────────────┐
│ Publish message                                             │
├─────────────────────────────────────────────────────────────┤
│ Routing key: [________________________]                     │
│ Headers: ? [________________________] = [________________]  │  [String ▼]
│ Properties: ? [_____________________] = [_______________]   │
│ Payload:                                                    │
│（在这填写消息体）                                              │
│ [________________________________________________________]  │
│                                                             │
│ Payload encoding: String (default) ▼                        │
│                                                             │
│ [Publish message]                                           │
└─────────────────────────────────────────────────────────────┘
```

这里是由控制台模拟了生产者发送的消息，由于没有消费者存在，最终消息就会丢失，这样也可以说明交换机没有存储消息的能力。

****
#### 2. 队列

打开 Queues 选项卡，新建一个队列，命名为 hello.queue1，再以相同的方式，创建一个队列，命名为 hello.queue2。最终队列列表如下：

| Overview    |          |          |          |          | Messages    |          |          | Message rates   |          |          |
|-------------|----------|----------|----------|----------|-------------|----------|----------|-----------------|----------|----------|
| Virtual host| Name     | Type     | Features | State    | Ready       | Unacked  | Total    | incoming        | deliver / get | ack      |
| /           | hello.queue1 | classic | D、Args | running  | 0           | 0        | 0        |                 |               |          |
| /           | hello.queue2 | classic | D、Args | running  | 0           | 0        | 0        |                 |               |          |

此时向 amq.fanout 交换机发送一条消息，会发现消息依然没有到达队列，因为发送到交换机的消息只会路由到与其绑定的队列，所以只创建队列是不够的，还需要将其与交换机绑定。
进入交换机页面，点击 Exchanges 选项卡，点击 amq.fanout 交换机，进入交换机详情页，然后点击 Bindings 菜单，在表单中填写要绑定的队列名称，然后再发送消息，可以发现消息发送成功。

****
### 2.3 数据隔离

#### 1. 用户管理

点击 Admin 选项卡，会看到 RabbitMQ 控制台的用户管理界面，这里的用户都是 RabbitMQ 的管理或运维人员，目前只有安装 RabbitMQ 时添加的 rabbitmq 这个用户。
关于用户表格中的字段：

- Name：rabbitmq，也就是用户名
- Tags：administrator，说明 rabbitmq 用户是超级管理员，拥有所有权限
- Can access virtual host：/，路径代表可以访问的 virtual host，这里的 / 是默认的 virtual host

对于小型企业而言，出于成本考虑，他们通常只会搭建一套 MQ 集群，公司内的多个不同项目同时使用。为了避免互相干扰，他们会利用 virtual host 的隔离特性，将不同项目隔离。
一般会做两件事情：

- 给每个项目创建独立的运维账号，将管理权限分离
- 给每个项目创建不同的virtual host，将每个项目的数据隔离

比如给黑马商城创建一个新的用户，点击 Add a user 并命名为 hmall，此时可以发现 hmall 用户没有任何 virtual host 的访问权限：

| Name     | Tags           | Can access virtual hosts  | Has password  |
|----------|----------------|---------------------------|---------------|
| hmall    | administrator  | No access（无访问权限）          | • （表示存在密码）    |
| rabbitmq | administrator  | /（可访问根虚拟主机 ）              | • （表示存在密码）    |

****
#### 2. virtual host

先退出登录，然后登录刚刚创建的 hmall，然后点击 Virtual Hosts 菜单，进入 virtual host 管理页，然后就可以看到目前只有一个默认的 virtual host，为 /：

以下是对该表格内容及各列含义的整理，用 Markdown 表格呈现并附带详细解释：

| Overview |          |          | Messages |          |          | Network |          | Message rates |          |
| ---------------- | -------- | -------- | -------------------- | -------- | -------- | -------------------- | -------- | ------------------------- | -------- |
| Name             | Users    | State    | Ready                | Unacked  | Total    | From client          | To client | publish                  | deliver / get |
| /                | rabbitmq | running  | 0                    | 0        | 0        |                      |          |                           |          |

此时点击 Add a new virtual host 给黑马商城项目创建一个单独的 virtual host，而不是使用默认的 /。由于目前登录的是 hmall 账户后创建的 virtual host，
所以回到 users 菜单，可以发现当前用户已经具备了对 /hmall 这个 virtual host 的访问权限了：

| Name     | Tags           | Can access virtual hosts  | Has password  |
|----------|----------------|---------------------------|---------------|
| hmall    | administrator  | No access（无访问权限）          | • （表示存在密码）    |
| rabbitmq | administrator  | /（可访问根虚拟主机 ）              | • （表示存在密码）    |

此时，点击页面右上角的 virtual host 下拉菜单，切换 virtual host 为 /hmall，然后查看 queues 选项卡，会发现之前的队列已经看不到了，这就是基于 virtual host 的隔离效果。

****
## 3. SpringAMQP

### 3.1 概述

未来开发业务功能的时候肯定不会在控制台收发消息，而是应该基于编程的方式，因为 RabbitMQ 采用了 AMQP 协议，所以它具备跨语言的特性。任何语言只要遵循 AMQP 协议收发消息，
都可以与 RabbitMQ 交互，并且 RabbitMQ 官方也提供了各种不同语言的客户端。而 Spring 官方刚好基于 RabbitMQ 提供了这样一套消息收发的模板工具：SpringAMQP，
并且还基于 SpringBoot 对其实现了自动装配。SpringAMQP  提供了三个功能：

- 自动声明队列、交换机及其绑定关系
- 基于注解的监听器模式，异步接收消息
- 封装了 RabbitTemplate 工具，用于发送消息

****
### 3.2 基本使用

在控制台新建一个 /hmall 下的队列：simple.queue，

#### 1. 消息发送

1、配置 MQ 地址，在 publisher 服务的 application.yml 中添加配置

```yaml
spring:
  rabbitmq:
    host: 127.0.0.1 # 你的虚拟机IP
    port: 5672 # 端口
    virtual-host: /hmall # 虚拟主机
    username: hmall # 用户名
    password: 123 # 密码
```

2、在 publisher 服务中编写测试类 SpringAmqpTest，并利用 RabbitTemplat e实现消息发送

```java
@Autowired
private RabbitTemplate rabbitTemplate;

@Test
public void testSimpleQueue() {
  // 队列名称
  String queueName = "simple.queue";
  // 消息
  String message = "hello, spring amqp!";
  // 发送消息
  rabbitTemplate.convertAndSend(queueName, message);
}
```

3、打开控制台，查看消息是否发送到队列中

****
#### 2. 消息接收

1、配置 MQ 地址，在 consumer 服务的 application.yml 中添加配置：

```yaml
spring:
  rabbitmq:
    host: 127.0.0.1 # 虚拟机IP
    port: 5672 # 端口
    virtual-host: /hmall # 虚拟主机
    username: hmall # 用户名
    password: 123 # 密码
```

2、在 consumer 服务的 com.itheima.consumer.listener 包中新建一个类 SpringRabbitListener

```java
@Component
public class SpringRabbitListener {
    // 利用 RabbitListener 来声明要监听的队列信息
    // 将来一旦监听的队列中有了消息，就会推送给当前服务，调用当前方法，处理消息
    // 可以看到方法体中接收的就是消息体的内容
    @RabbitListener(queues = "simple.queue")
    public void listenSimpleQueueMessage(String msg) {
        System.out.println("spring 消费者接收到消息：[" + msg + "]");
    }
}
```

****
### 3.3 WorkQueues 模型

Work queues 任务模型，简单来说就是让多个消费者绑定到一个队列，共同消费队列中的消息。一般情况下，都是一个消费者处理一个队列的，但是当消息处理较久时，可能生产消息的速度会远远大于消费的速度，
时间久了消息就会堆积越来越多，导致无法及时处理。此时就可以使用 Work queues 模型，让多个消费者共同处理消息处理，消息处理的速度就能大大提高了。

利用循环发送消息模拟大量消息堆积现象：

```java
@Test
public void testWorkQueue() throws InterruptedException {
    // 队列名称
    String queueName = "work.queue";
    // 消息
    String message = "hello, message_";
    for (int i = 0; i < 50; i++) {
        // 发送消息，每20毫秒发送一次，相当于每秒发送50条消息
        rabbitTemplate.convertAndSend(queueName, message + i);
        Thread.sleep(20);
    }
}
```

在 consumer 服务的 SpringRabbitListener 中添加 2 个新的方法:

```java
@RabbitListener(queues = "work.queue")
public void listenWorkQueue1(String msg) throws InterruptedException {
  System.out.println("消费者1接收到消息：[" + msg + "]" + LocalTime.now());
  Thread.sleep(20);
}

@RabbitListener(queues = "work.queue")
public void listenWorkQueue2(String msg) throws InterruptedException {
  System.err.println("消费者2........接收到消息：[" + msg + "]" + LocalTime.now());
  Thread.sleep(200);
}
```

这两消费者，都设置了 Thead.sleep，模拟任务耗时：

- 消费者1 sleep 了 20 毫秒，相当于每秒钟处理 50 个消息
- 消费者2 sleep 了 200 毫秒，相当于每秒处理 5 个消息

根据最后的打印结果来看，这两个消费者处理的信息条数一致，当消费者1 处理完 25 个消息后，就不再接收消息了。RabbitMQ 在不做特别配置时，消息的分发策略是简单轮询，每次来一条消息，
就交替推送给每个消费者，而不考虑它们的处理能力，最终两个消费者各自获得一半的消息。如果需要启用 "能者多劳" 模式的话，就要进行相关配置：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        prefetch: 1 # 每次只能获取一条消息，处理完成才能获取下一个消息
```

****
### 3.4 Fanout 交换机

在上面的测试中，没有用到交换机，而是直接把消息发送到队列，而交换机的类型有四种：

- Fanout：广播，将消息交给所有绑定到交换机的队列。我们最早在控制台使用的正是 Fanout 交换机
- Direct：订阅，基于 RoutingKey（路由 key）发送给订阅了消息的队列
- Topic：通配符订阅，与 Direct 类似，只不过 RoutingKey 可以使用通配符
- Headers：头匹配，基于 MQ 的消息头匹配，用的较少

在广播模式下，消息发送流程是这样的：

- 1）可以有多个队列
- 2）每个队列都要绑定到 Exchange（交换机）
- 3）生产者发送的消息，只能发送到交换机
- 4）交换机把消息发送给绑定过的所有队列
- 5）订阅队列的消费者都能拿到消息

在控制台创建队列 fanout.queue1 和 fanout.queue2，然后再创建一个交换机 hmall.fanout 并绑定这两个队列，然后添加消息发送测试方法：

```java
@Test
public void testFanoutExchange() {
    // 交换机名称
    String exchangeName = "hmall.fanout";
    // 消息
    String message = "hello, everyone!";
    rabbitTemplate.convertAndSend(exchangeName, "", message); // 第一个参数为交换机，第二个为 Routing Key，第三个为消息
}
```

添加消息消费者：

```java
@RabbitListener(queues = "fanout.queue1")
public void listenFanoutQueue1(String msg) {
    System.out.println("消费者1接收到Fanout消息：[" + msg + "]");
}

@RabbitListener(queues = "fanout.queue2")
public void listenFanoutQueue2(String msg) {
    System.out.println("消费者2接收到Fanout消息：[" + msg + "]");
}
```

当生产者将消息发送到 Fanout 类型的交换机时，交换机不会考虑消息的 Routing Key，它会无条件地将消息复制并投递到所有与它绑定的队列中，也就是说只要有队列绑定到这个 Fanout 交换机，
就会收到消息副本。而交换机的作用就是：

- 接收 publisher 发送的消息
- 将消息按照规则路由到与之绑定的队列
- 不能缓存消息，路由失败，消息丢失
- FanoutExchange 的会将消息路由到每个绑定的队列

****
### 3.5 Direct 交换机

在 Fanout 模式中，一条消息会被所有订阅的队列都消费。但是在某些场景下，不同的消息应该被不同的队列消费，这时就要用到 Direct 类型的 Exchange。在 Direct 模型下：

- 队列与交换机的绑定不能再是任意绑定的了，而是要指定一个 Routing Key（路由 key）
- 消息的发送方在 向 Exchange 发送消息时，也必须指定消息的 Routing Key。
- Exchange 不再把消息交给每一个绑定的队列，而是根据消息的 Routing Key 进行判断，只有队列的 Routing Key 与消息的 Routing Key 完全一致，才会接收到消息

例如：

1. 声明一个名为 hmall.direct 的交换机
2. 声明队列 direct.queue1，绑定 hmall.direct，Routing Key 为 blue 和 red
3. 声明队列 direct.queue2，绑定 hmall.direct，Routing Key 为 yellow 和 red
4. 在 consumer 服务中，编写两个消费者方法，分别监听 direct.queue1 和 direct.queue2
5. 在 publisher 中编写测试方法，向 hmall.direct 发送消息 

消息发送：

```java
@Test
public void testSendDirectExchange() {
    // 交换机名称
    String exchangeName = "hmall.direct";
    // 消息
    String message = "红色警报！日本乱排核废水，导致海洋生物变异，惊现哥斯拉！";
    // 发送消息
    rabbitTemplate.convertAndSend(exchangeName, "red", message);
}
```

消息接收：

```java
@RabbitListener(queues = "direct.queue1")
public void listenDirectQueue1(String msg) {
    System.out.println("消费者1接收到direct.queue1的消息：[" + msg + "]");
}

@RabbitListener(queues = "direct.queue2")
public void listenDirectQueue2(String msg) {
    System.out.println("消费者2接收到direct.queue2的消息：[" + msg + "]");
}
```

因为发送消息时制定了 Routing Key 为 red，所以只有为 red 的队列可以接收到消息，而创建的这两个队列的 Routing Key 都是 red，所以它们都能接收到消息：

```text
消费者 1 接收到 direct.queue1 的消息：[红色警报！日本乱排核废水，导致海洋生物变异，惊现哥斯拉！]
消费者 2 接收到 direct.queue2 的消息：[红色警报！日本乱排核废水，导致海洋生物变异，惊现哥斯拉！]
```

如果更换 Routing Key 为 blue，那就只有 direct.queue1 可以接收到消息：

```text
消费者 1 接收到 direct.queue1 的消息：[蓝色警报！日本拒绝承认造成海洋污染！]
```

Direct 与 Fanout 的差异

- Fanout 交换机将消息路由给每一个与之绑定的队列
- Direct 交换机根据 Routing Key 判断路由给哪个队列
- 如果多个队列具有相同的 Routing Key，则与 Fanout 功能类似

****
### 3.6 Topic 交换机

Topic 与 Direct 类似，都是可以根据 RoutingKey 把消息路由到不同的队列，只不过 Topic 可以让队列在绑定 Routing Key 的时候使用通配符，Routing Key 一般都是有一个或多个单词组成，
多个单词之间以 "." 分割，例如： item.insert。通配符规则：

- #：匹配一个或多个词
- *：匹配不多不少恰好 1 个词

例如：

- item.#：能够匹配 item.spu.insert 或者 item.spu
- item.*：只能匹配 item.spu

假如此时 publisher 发送的消息使用的 Routing Key 共有四种：

- china.news 代表有中国的新闻消息
- china.weather 代表中国的天气消息
- japan.news 则代表日本新闻
- japan.weather 代表日本的天气消息

创建两个队列 topic.queue1 和 topic.queue2

- topic.queue1：绑定的是 china.# ，凡是以 china. 开头的 routing key 都会被匹配到，包括：
  - china.news
  - china.weather
- topic.queue2：绑定的是 #.news ，凡是以 .news 结尾的 routing key 都会被匹配，包括:
  - china.news
  - japan.news

消息发送：

```java
@Test
public void testSendTopicExchange() {
    // 交换机名称
    String exchangeName = "hmall.topic";
    // 消息
    String message = "喜报！孙悟空大战哥斯拉，胜!";
    // 发送消息
    rabbitTemplate.convertAndSend(exchangeName, "china.news", message);
}
```

消息接收：

```java
@RabbitListener(queues = "topic.queue1")
public void listenTopicQueue1(String msg){
  System.out.println("消费者 1 接收到 topic.queue1 的消息：[" + msg + "]");
}

@RabbitListener(queues = "topic.queue2")
public void listenTopicQueue2(String msg){
  System.out.println("消费者 2 接收到 topic.queue2 的消息：[" + msg + "]");
}
```

这两个队列都能接收到 china.news 的消息：

```text
消费者 2 接收到 topic.queue2 的消息：[喜报！孙悟空大战哥斯拉，胜!]
消费者 1 接收到 topic.queue1 的消息：[喜报！孙悟空大战哥斯拉，胜!]
```

****
### 3.7 SpringBoot 声明队列和交换机

#### 1. 基于配置类

目前都是基于 RabbitMQ 控制台来创建队列、交换机，但是在实际开发时，队列和交换机是程序员定义的，将来项目上线，又要交给运维去创建。那么程序员就需要把程序中运行的所有队列和交换机都写下来，
交给运维，但是在这个过程中是很容易出现错误的，因此推荐的做法是由程序启动时检查队列和交换机是否存在，如果不存在自动创建。

SpringAMQP 提供了一个 Queue 类，用来创建队列；SpringAMQP 还提供了一个 Exchange 接口，来表示所有不同类型的交换机。可以通过这些接口自己创建队列和交换机，
不过 SpringAMQP 还提供了 ExchangeBuilder 来简化这个过程。而在绑定队列和交换机时，则需要使用 BindingBuilder 来创建 Binding 对象，用于声明队列和交换机的绑定关系。

以 Fanout 交换机为例，在 consumer 模块创建。[Direct](./src/main/java/com/itheima/consumer/config/DirectConfig.java) 交换机同理：

```java
@Configuration
public class FanoutConfig {

    // 声明交换机
    @Bean
    public FanoutExchange fanoutExchange(){
        // return new FanoutExchange("hmall.fanout");
        return ExchangeBuilder.fanoutExchange("hmall.fanout").build();
    }

    @Bean
    public Queue fanoutQueue1(){
        // 默认开启持久化，即在磁盘储存
        return new Queue("fanout.queue1");
        // return QeueBuilder.durable("fanout.queue1").build();
    }

    // 绑定队列和交换机
    @Bean
    public Binding bindingQueue1(Queue fanoutQueue1, FanoutExchange fanoutExchange){
        return BindingBuilder.bind(fanoutQueue1).to(fanoutExchange);
    }

    @Bean
    public Queue fanoutQueue2(){
        return new Queue("fanout.queue2");
    }
    
    @Bean
    public Binding bindingQueue2(Queue fanoutQueue2, FanoutExchange fanoutExchange){
        return BindingBuilder.bind(fanoutQueue2).to(fanoutExchange);
    }
}
```

****
#### 2. 基于注解

基于配置类的方式声明队列和交换机比较麻烦，Spring 还提供了基于注解方式来声明，以 Direct 为例：

```java
@RabbitListener(bindings = @QueueBinding(
        value = @Queue(name = "direct.queue1"),
        exchange = @Exchange(name = "hmall.direct", type = ExchangeTypes.DIRECT),
        key = {"red", "blue"}
))
public void listenDirectQueue1ByAnnotation(String msg){
  System.out.println("消费者 1 接收到 direct.queue1 的消息：[" + msg + "]");
}

@RabbitListener(bindings = @QueueBinding(
        value = @Queue(name = "direct.queue2"),
        exchange = @Exchange(name = "hmall.direct", type = ExchangeTypes.DIRECT),
        key = {"red", "yellow"}
))
public void listenDirectQueue2ByAnnotation(String msg){
  System.out.println("消费者 2 接收到 direct.queue2 的消息：[" + msg + "]");
}
```

****
### 3.8 消息转换器

Spring 的消息发送代码接收的消息体是一个 Object，也就是说可以发送任意的消息对象：

```java
public void convertAndSend(String exchange, String routingKey, Object object) throws AmqpException {
    this.convertAndSend(exchange, routingKey, object, (CorrelationData)null);
}
```

而它是依赖网络传输的，所以数据传输时它会把发送的消息序列化为字节再发送给 MQ，接收消息的时候，还会把字节反序列化为 Java 对象。只不过默认情况下 Spring 采用的序列化方式是 JDK 序列化，
而 JDK 序列化存在下列问题：

- 数据体积过大
- 有安全漏洞
- 可读性差

#### 1. 测试默认转换器

1、在 consumer 服务中声明一个新的配置类：

先不给这个队列添加消费者，查看一下消息体的格式。

```java
@Configuration
public class MessageConfig {
    @Bean
    public Queue objectQueue() {
        return new Queue("object.queue");
    }
}
```

2、发送消息

```java
@Test
public void testSendMap() throws InterruptedException {
    // 准备消息
    Map<String,Object> msg = new HashMap<>();
    msg.put("name", "jack");
    msg.put("age", 21);
    // 发送消息
    rabbitTemplate.convertAndSend("object.queue", msg);
}
```

3、发送消息后查看控制台

```text
rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAACdAAEbmFtZXQA
Buafs+WyqXQAA2FnZXNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAV
eA==
```

在上面的 convertAndSend 方法中，它会继续进入：

```java
public void convertAndSend(String exchange, String routingKey, Object object, @Nullable CorrelationData correlationData) throws AmqpException {
    this.send(exchange, routingKey, this.convertMessageIfNecessary(object), correlationData);
}
```

```java
protected Message convertMessageIfNecessary(Object object) {
    if (object instanceof Message msg) {
        return msg;
    } else {
        return this.getRequiredMessageConverter().toMessage(object, new MessageProperties());
    }
}
```

然后就可以找到它默认使用的是一个 SimpleMessageConverter 消息转换器：

```java
private MessageConverter messageConverter = new SimpleMessageConverter();
```

继续跟入 toMessage() 方法，最终进入 SimpleMessageConverter 的 createMessage() 方法：

```java
protected Message createMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
    if (object instanceof byte[] bytes) {
        messageProperties.setContentType("application/octet-stream");
    } else if (object instanceof String) {
        try {
            bytes = ((String)object).getBytes(this.defaultCharset);
        } catch (UnsupportedEncodingException e) {
            throw new MessageConversionException("failed to convert to Message content", e);
        }

        messageProperties.setContentType("text/plain");
        messageProperties.setContentEncoding(this.defaultCharset);
    } else if (object instanceof Serializable) {
        try {
            bytes = SerializationUtils.serialize(object);
        } catch (IllegalArgumentException e) {
            throw new MessageConversionException("failed to convert to serialized Message content", e);
        }

        messageProperties.setContentType("application/x-java-serialized-object");
    }

    if (bytes != null) {
        messageProperties.setContentLength((long)bytes.length);
        return new Message(bytes, messageProperties);
    } else {
        String var10002 = this.getClass().getSimpleName();
        throw new IllegalArgumentException(var10002 + " only supports String, byte[] and Serializable payloads, received: " + object.getClass().getName());
    }
}
```

它的作用就是：把常见类型的对象（String、byte[]、Serializable）转换成消息（Message）:

```java
if (object instanceof byte[] bytes) {
    messageProperties.setContentType("application/octet-stream");
} else if (object instanceof String) {
    try {
        bytes = ((String)object).getBytes(this.defaultCharset);
    } catch (UnsupportedEncodingException e) {
        throw new MessageConversionException("failed to convert to Message content", e);
    }

    messageProperties.setContentType("text/plain");
    messageProperties.setContentEncoding(this.defaultCharset);
}
```

如果原本就是字节数组，不做处理，直接用；如果是 String 类型，则转为字节数组，需要注意的是，它这里设置了默认的编码：messageProperties.setContentEncoding(this.defaultCharset);
也就是说，前端在请求头中看到了编码为 UTF-8，则会自动将字节数组转换成 String，所以为什么传递 String 的时候在 RabbitMQ 控制台仍然能看到正常的内容；
如果是对象类型：

```java
else if (object instanceof Serializable) {
    try {
        bytes = SerializationUtils.serialize(object);
    } catch (IllegalArgumentException e) {
        throw new MessageConversionException("failed to convert to serialized Message content", e);
    }

    messageProperties.setContentType("application/x-java-serialized-object");
}
```

就对该对象进行序列化，而序列化的本质就是把对象转换成字节数组，便于网络传输。而在 RabbitMQ 页面也可以看到请求头的格式：

```text
headers:	
content_type:	application/x-java-serialized-object
```

****
#### 2. 配置 JSON 转换器

默认的 JDK 序列化方式并不合适发送字符串以外的类型，如果希望消息体的体积更小、可读性更高，就应该使用 JSON 方式来做序列化和反序列化。在 publisher 和 consumer 两个服务中都引入依赖：

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-json</artifactId>
</dependency>
<dependency>
  <groupId>com.fasterxml.jackson.dataformat</groupId>
  <artifactId>jackson-dataformat-xml</artifactId>
</dependency>
```

配置消息转换器，在 publisher 和 consumer 两个服务的启动类中添加一个 Bean 即可：

```java
@Bean
public MessageConverter messageConverter(){
    // 1. 定义消息转换器
    Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
    // 2. 配置自动创建消息 id，用于识别不同消息，也可以在业务中基于 ID 判断是否是重复消息
    jackson2JsonMessageConverter.setCreateMessageIds(true); // 底层使用 UUID 可以判断后续消费者是否重复消费一条消息
    return jackson2JsonMessageConverter;
}
```

控制页面正常显示 Json 内容：

```text
headers:	
__ContentTypeId__:	java.lang.Object
__KeyTypeId__:	java.lang.Object
__TypeId__:	java.util.HashMap
content_encoding:	UTF-8
content_type:	application/json
```

```json
{"name":"jack","age":21}
```

****
## 4. 使用 RabbitMQ 改造业务

原始业务流程：

```text
[客户端] --> [支付服务] --> [订单服务]  --> 返回响应
```

使用 MQ 后：

```text
[客户端] --> [支付服务] ---MQ---> [订单服务]
```

1、配置 MQ

为生产者和消费者添加依赖并配置 MQ 地址：

```xml
<!--消息发送-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

```yaml
spring:
  rabbitmq:
    host: 127.0.0.1 # 虚拟机IP
    port: 5672 # 端口
    virtual-host: /hmall # 虚拟主机
    username: hmall # 用户名
    password: 123 # 密码
```

2、添加消息转换器

在 hm-common 模块下新建 MqConfig 配置类配置消息转换器，并把它归纳进 org.springframework.boot.autoconfigure.AutoConfiguration.imports 文件中：

```java
@Configuration
public class MqConfig {
    @Bean
    public MessageConverter messageConverter() {
        // 1. 定义消息转换器    
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
        // 2. 配置自动创建消息 id，用于识别不同消息，也可以在业务中基于 ID 判断是否是重复消息    
        jackson2JsonMessageConverter.setCreateMessageIds(true);
        return jackson2JsonMessageConverter;
    }
}
```

3、接收消息

在 trade-service 模块中添加消息监听类，当监听到消息时就触发下单成功功能：

```java
@Component
@RequiredArgsConstructor
public class listenerPayStatusListener {
    private final IOrderService orderService;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "trade.pay.success.queue", durable = "true"),
            exchange = @Exchange(name = "pay.topic"),
            key = "pay.success"
    ))
    public void listenPaySuccess(Long orderId){
        orderService.markOrderPaySuccess(orderId);
    }
}
```

4、发送消息

修改 pay-service 服务下的 PayOrderServiceImpl 类中的 tryPayOrderByBalance 方法：

```java
private final RabbitTemplate rabbitTemplate;

@Override
@Transactional
public void tryPayOrderByBalance(PayOrderDTO payOrderDTO) {
    // 1.查询支付单
    PayOrder po = getById(payOrderDTO.getId());
    // 2.判断状态
    if(!PayStatus.WAIT_BUYER_PAY.equalsValue(po.getStatus())){
        // 订单不是未支付，状态异常
        throw new BizIllegalException("交易已支付或关闭！");
    }
    // 3.尝试扣减余额
    userClient.deductMoney(payOrderDTO.getPw(), po.getAmount());
    // 4.修改支付单状态
    boolean success = markPayOrderSuccess(payOrderDTO.getId(), LocalDateTime.now());
    if (!success) {
        throw new BizIllegalException("交易已支付或关闭！");
    }
    // 5.修改订单状态
    // tradeClient.markOrderPaySuccess(po.getBizOrderNo());
    try {
        rabbitTemplate.convertAndSend("pay.direct", "pay.success", po.getBizOrderNo());
    } catch (Exception e) {
        log.error("支付成功的消息发送失败，支付单id：{}， 交易单id：{}", po.getId(), po.getBizOrderNo(), e);
    }
}
```

这里不再是通过 OpenFeign 远程调用别的微服务了，而是把自己的订单 id 作为消息转发给订单微服务。

****
# 七、MQ 高级

在上面的功能改造中，在支付成功后利用 RabbitMQ 通知交易服务，然后更新业务订单状态为已支付，但是如果 MQ 通知失败，也就是完成了支付功能，而订单修改功能未执行，
就会出现数据不一致的现象，而查询订单的请求也无法查看到刚刚支付成功的订单。所以，为了保证 MQ 消息的可靠性，必须要确保发送的消息至少被消费一次。

## 1. 发送者的可靠性

消息从生产者到消费者的每一步都可能导致消息丢失：

- 发送消息时丢失：
  - 生产者发送消息时连接 MQ 失败
  - 生产者发送消息到达 MQ 后未找到 Exchange
  - 生产者发送消息到达 MQ 的 Exchange 后，未找到合适的 Queue
  - 消息到达 MQ 后，处理消息的进程发生异常
  
- MQ 导致消息丢失：
  - 消息到达 MQ，保存到队列后，尚未消费就突然宕机
  
- 消费者处理消息时：
  - 消息接收后尚未处理突然宕机
  - 消息接收后处理过程中抛出异常

所以为了保证可靠性，就必须确保：

- 确保生产者一定把消息发送到 MQ
- 确保 MQ 不会将消息弄丢
- 确保消费者一定要处理消息

#### 1. 生产者重试机制

首先第一种情况，就是生产者发送消息时出现了网络故障，导致与 MQ 的连接中断。而 SpringAMQP 提供了消息发送时的重试机制，即当 RabbitTemplate 与 MQ 连接超时后，多次重试。
修改 publisher 模块的 application.yaml 文件，添加下面的内容：

```yaml
spring:
  rabbitmq:
    connection-timeout: 1s # 设置MQ的连接超时时间
    template:
      retry:
        enabled: true # 开启超时重试机制
        initial-interval: 1000ms # 失败后的初始等待时间
        multiplier: 1 # 失败后下次的等待时长倍数，下次等待时长 = initial-interval * multiplier
        max-attempts: 3 # 最大重试次数
```

用 Docker 关闭 mq 后再执行发送消息，它会进行三次尝试，每次等待 1 s。但需要注意的是：当网络不稳定的时候，利用重试机制可以有效提高消息发送的成功率，
但 SpringAMQP 提供的重试机制是阻塞式的重试，也就是说多次重试等待的过程中，当前线程是被阻塞的。如果对于业务性能有要求，建议禁用重试机制，如果一定要使用，
需要合理配置等待时长和重试次数，当然也可以考虑使用异步线程来执行发送消息的代码。

****
#### 2. 生产者确认机制

一般情况下，只要生产者与 MQ 之间的网路连接顺畅，基本不会出现发送消息丢失的情况，因此大多数情况下无需考虑这种问题。但偶尔也会出现消息发送到MQ之后丢失的现象，比如：

- MQ 内部处理消息的进程发生了异常
- 生产者发送消息到达 MQ 后未找到 Exchange
- 生产者发送消息到达 MQ 的 Exchange 后，未找到合适的 Queue，因此无法路由

针对上述情况，RabbitMQ 提供了生产者消息确认机制，包括 Publisher Confirm 和 Publisher Return 两种。在开启确认机制的情况下，当生产者发送消息给 MQ 后，
MQ 会根据消息处理的情况返回不同的回执：

- 当消息投递到 MQ，但是路由失败时，通过 Publisher Return 返回异常信息，同时返回 ack 的确认信息，代表投递成功
- 临时消息投递到了 MQ，并且入队成功，返回 ACK，告知投递成功
- 持久消息投递到了 MQ，并且入队完成持久化，返回 ACK，告知投递成功
- 其它情况都会返回 NACK，告知投递失败

其中 ack 和 nack 属于 Publisher Confirm 机制，ack 是投递成功；nack 是投递失败，而 return 则属于 Publisher Return 的机制。在 RabbitMQ 中，生产者将消息发送到交换机，
如果该交换机找不到对应的队列，就会发生消息被退回（Return）的情况，默认情况下，RabbitMQ 会直接丢弃这类消息，不通知生产者，为了避免消息丢失，
可以通过配置文件 + ReturnsCallback 来接收这些 "退回" 的消息。

RabbitMQ 的消息从生产者到消费者之间分为多个阶段，其中生产者投递阶段重点包括：

1. 消息是否成功到达交换机（Exchange） -> 通过 Publisher Confirm（ACK / NACK）检测
2. 消息是否成功路由到队列（Queue） -> 通过 ReturnCallback 检测

##### 2.1 定义 ReturnCallback

在 publisher 模块的 application.yaml 中添加配置，开启生产者确认：

```yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated # 开启publisher confirm机制，并设置confirm类型
    publisher-returns: true # 开启publisher return机制
```

publisher-confirm-type 有三种模式可选：

- none：关闭 confirm 机制
- simple：同步阻塞等待 MQ 的回执
- correlated：MQ 异步回调返回回执

或者使用 rabbitTemplate.setMandatory(true)，如果写了配置文件，则会自动注册 Mandatory。每个 RabbitTemplate 只能配置一个 ReturnCallback，
因此可以在配置类中统一设置，由 SpringBoot 启动时自动注入，在 publisher 模块定义一个配置类：

```java
@Slf4j
@AllArgsConstructor
@Configuration
public class MqConfig {
    private final RabbitTemplate rabbitTemplate;
    @PostConstruct
    public void init(){
        // rabbitTemplate.setMandatory(true); // 开启“路由失败”回调机制

        // 注册 ReturnCallback 回调
        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
            @Override
            public void returnedMessage(ReturnedMessage returned) {
                log.error("触发 return callback,");
                log.debug("exchange: {}", returned.getExchange());
                log.debug("routingKey: {}", returned.getRoutingKey());
                log.debug("message: {}", returned.getMessage());
                log.debug("replyCode: {}", returned.getReplyCode());
                log.debug("replyText: {}", returned.getReplyText());
            }
        });
    }
}
```

- `rabbitTemplate.setReturnsCallback(...)`：设置一个回调函数，如果 RabbitMQ 无法路由消息到任何队列，就会执行这里的代码
- `replyCode`：RabbitMQ 返回的拒收码，常见的是 312，表示 NO_ROUTE
- `replyText`：拒收原因说明，通常是 "NO_ROUTE" 表示无匹配路由

****
##### 2.2 定义 ConfirmCallback

每次调用 RabbitTemplate.convertAndSend(...) 方法发送消息时，可以通过传入 CorrelationData 来为这条消息绑定一个唯一 ID 和一个回执的异步处理器（CompletableFuture），
用于接收 RabbitMQ 的确认回执：

```java
convertAndSend(String exchange, String routingKey, Object message, CorrelationData correlationData);
```

其中，CorrelationData 是 Spring AMQP 提供的一个对象，用来标识一条消息的唯一性，并且承载消息的回执信息（Future 对象），其中包含两个核心内容：

- id：消息的唯一标识，RabbitMQ 将基于此字段返回相应的 ack/nack 回执（之前定义 MqConfig 时设置的 jackson2JsonMessageConverter.setCreateMessageIds(true);）
- CompletableFuture<CorrelationData.Confirm>：用于异步接收 MQ 回执结果

并且可以使用 CompletableFuture.whenComplete(...) 给其添加回调来处理消息的投递确认（SpringBoot 3.x/Spring 6.x 启用的），这个方法的两个参数：

- confirm: 是 RabbitMQ 的异步确认结果（类型是 CorrelationData.Confirm）
- ex: 是如果发生异常（如网络问题等），则会带上异常对象 
- confirm.isAck() == true：说明 RabbitMQ 已成功收到并确认该消息，打印 debug 成功日志
- confirm.isAck() == false：说明 RabbitMQ 拒收或失败，需要查看 confirm.getReason() 获取失败原因
```java
@Test
void testPublisherConfirm() {
  // 1. 创建 CorrelationData
  CorrelationData cd = new CorrelationData();
  // 2. 给 CompletableFuture 添加回调
  cd.getFuture().whenComplete((confirm, ex) -> {
    if (ex != null) {
      // Future 发生异常（基本不会发生）
      log.error("send message fail", ex);
    } else {
      if (confirm != null && confirm.isAck()) {
        log.debug("发送消息成功，收到 ack!");
      } else {
        log.error("发送消息失败，收到 nack, reason: {}", confirm != null ? confirm.getReason() : "null confirm");
      }
    }
  });
  // 3. 发送消息
  rabbitTemplate.convertAndSend("hmall.direct", "wrong", "hello", cd);
}
```

这里绑定了一个不存在的 Routing Key，模拟找不到队列：

```text
触发 return callback,
发送消息成功，收到 ack!
exchange: hmall.direct
routingKey: wrong
message: (Body:'"hello"' MessageProperties [headers={spring_returned_message_correlation=92ac7289-7c21-4afc-8acb-e4e113864f1d, __TypeId__=java.lang.String}, messageId=e43dd3c8-1c55-401e-9948-64852231b601, contentType=application/json, contentEncoding=UTF-8, contentLength=0, receivedDeliveryMode=PERSISTENT, priority=0, deliveryTag=0])
replyCode: 312
replyText: NO_ROUTE
```

最终打印结果只输出了 "触发 return callback,"，而定义在 MqConfig 中的其它信息没有打印，证明发送消息失败。如果绑定正确的 Routing Key：

```text
消费者 1 接收到 direct.queue1 的消息：[hello]
------------------------------------------
发送消息成功，收到 ack!
```

可以看到，由于传递的 Routing Key 是错误的，路由失败后，触发了 return callback，同时也收到了 ack。当修改为正确的 Routing Key 以后，就不会触发 return callback 了，
只收到 ack，而如果连交换机都是错误的，则只会收到 nack。

需要注意的是：开启生产者确认比较消耗 MQ 性能，一般不建议开启。

- 路由失败：一般是因为 Routing Key 错误导致，往往是编程导致
- 交换机名称错误：同样是编程错误导致
- MQ 内部故障：这种需要处理，但概率往往较低，因此只有对消息可靠性要求非常高的业务才需要开启，所以只需要开启 ConfirmCallback 处理 nack 就可以了

| 功能                  | 描述                  | 性能影响 | 是否推荐             |
|---------------------| ------------------- | ---- | ---------------- |
| `Publisher Confirm` | 用于检测消息是否到达 Exchange | 高    | 仅推荐对可靠性要求高的业务开启  |
| `ReturnCallback`    | 检测是否成功路由到 Queue     | 较小   | 可视情况开启（调试阶段非常有用） |

****
## 2. MQ 的可靠性

### 2.1 数据持久化

为了提升性能，默认情况下 MQ 的数据都是在内存存储的临时数据，重启后就会消失。为了保证数据的可靠性，必须配置数据持久化，包括：

- 交换机持久化
- 队列持久化
- 消息持久化

在控制台的 Exchanges 页面，添加交换机时可以配置交换机的 Durability 参数，设置为 Durable 就是持久化模式，交换机会被持久化到磁盘，在 RabbitMQ 重启后依然存在，
Transient 就是临时模式，重启后失效；在控制台的 Queues 页面，添加队列时同样可以配置队列的 Durability 参数；在控制台发送消息的时候，可以添加很多参数，
deliveryMode 属性用来控制持久化，deliveryMode = 2（持久化）、deliveryMode = 1（非持久化）。

需要注意的是，在程序中发送的消息默认是持久化的，如果需要开启非持久化，就需要手动创建 Message：

```java
@Test
void testSendMessage() {
    // 自定义构建消息
    MessageBuilder.withBody("hello world!".getBytes(StandardCharsets.UTF_8)).setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
    // 发送消息
    for (int i = 0; i < 1000000; i++) {
        rabbitTemplate.convertAndSend("hmall.direct", "red", "hello world!");
    }
}
```

非持久化消息通常传输效率更高，但当消息堆积导致内存耗尽时，RabbitMQ 可能会把非持久化消息存到磁盘临时保存，这时反而导致性能下降甚至阻塞。持久化消息从一开始就需要写磁盘，
因此性能天然就比非持久化消息低，但可靠性高。

在开启持久化机制以后，如果同时还开启了生产者确认，那么 MQ 会在消息持久化以后才发送 ACK 回执，进一步确保消息的可靠性。
不过出于性能考虑，为了减少 IO 次数，发送到 MQ 的消息并不是逐条持久化到数据库的，而是每隔一段时间批量持久化，一般间隔在 100 毫秒左右，这就会导致 ACK 有一定的延迟，
因此建议生产者确认全部采用异步方式，防止发送线程阻塞等待 ACK。而发送消息 convertAndSend 方法是非阻塞的，消息发送后立即返回，所以上面的 ConfirmCallback 本质上就是异步的。

****
### 2.2 LazyQueue

在默认情况下，RabbitMQ 会将接收到的信息保存在内存中以降低消息收发的延迟。但在某些特殊情况下，这会导致消息积压，比如：

- 消费者宕机或出现网络故障
- 消息发送量激增，超过了消费者处理速度
- 消费者处理业务发生阻塞

一旦出现消息堆积问题，RabbitMQ 的内存占用就会越来越高，直到触发内存预警上限。此时 RabbitMQ 会将内存消息刷到磁盘上，这个行为成为 PageOut，PageOut 会耗费一段时间，
并且会阻塞队列进程，因此在这个过程中 RabbitMQ 不会再处理新的消息，生产者的所有请求都会被阻塞。从 RabbitMQ 的3.6.0 版本开始，就增加了 Lazy Queues 的模式，
也就是惰性队列，而在 3.12 版本之后，LazyQueue 已经成为所有队列的默认格式：

- 接收到消息后直接存入磁盘而非内存
- 消费者要消费消息时才会从磁盘中读取并加载到内存（也就是懒加载）
- 支持数百万条的消息存储

在添加队列的时候，在 Arguments 处添加 x-queue-mod=lazy 参数即可设置队列为 Lazy 模式，在利用 SpringAMQP 声明队列的时候，
添加 x-queue-mod=lazy 参数也可设置队列为 Lazy 模式：

```java
@Bean
public Queue lazyQueue(){
    return QueueBuilder
            .durable("lazy.queue")
            .lazy() // 开启Lazy模式
            .build();
}
```

也可以基于注解来声明队列并设置为 Lazy 模式:

```java
@RabbitListener(queuesToDeclare = @Queue(
        name = "lazy.queue",
        durable = "true",
        arguments = @Argument(name = "x-queue-mode", value = "lazy")
))
public void listenLazyQueue(String msg){
    log.info("接收到 lazy.queue的消息：{}", msg);
}
```

对于已经存在的队列，也可以配置为 lazy 模式，但是要通过设置 policy 实现，可以基于命令行设置 policy：

```shell
rabbitmqctl set_policy Lazy "^lazy-queue$" '{"queue-mode":"lazy"}' --apply-to queues  
```

- rabbitmqctl ：RabbitMQ 的命令行工具
- set_policy ：添加一个策略
- Lazy ：策略名称，可以自定义
- "^lazy-queue$" ：用正则表达式匹配队列的名字，只作用于名为 lazy-queue 的队列，使用 ".*" 作用域所有队列
- '{"queue-mode":"lazy"}' ：设置队列模式为 lazy 模式
- --apply-to queues：策略的作用对象，是所有的队列

因为是在 Docker 中安装的，所以要进入 Docker 的容器：

```shell
docker exec -it rabbitmq bash
rabbitmqctl set_policy Lazy "^lazy-queue$" '{"queue-mode":"lazy"}' --apply-to queues
```

也可以在控制台配置 policy，进入在控制台的 Admin 页面，点击 Policies，即可添加配置

****
### 2.3 消费者的可靠性

当 RabbitMQ 向消费者投递消息以后，需要知道消费者的处理状态如何。因为消息投递给消费者并不代表就一定被正确消费了，可能出现的故障有很多，比如：

- 消息投递的过程中出现了网络故障
- 消费者接收到消息后突然宕机
- 消费者接收到消息后，因处理不当导致异常

#### 1. 消费者确认机制

为了确认消费者是否成功处理消息，RabbitMQ 提供了消费者确认机制（Consumer Acknowledgement）。即：当消费者处理消息结束后，应该向 RabbitMQ 发送一个回执，
告知 RabbitMQ 消息处理的状态，回执有三种可选值：

- ack：成功处理消息，RabbitMQ 从队列中删除该消息
- nack：消息处理失败，RabbitMQ 需要再次投递消息
- reject：消息处理失败并拒绝该消息，RabbitMQ 从队列中删除该消息

一般 reject 方式用的较少，除非是消息格式有问题，否则就是开发问题了。所以大多数情况下需要将消息处理的代码通过 try catch 机制捕获，消息处理成功时返回 ack，
处理失败时返回 nack。由于消息回执的处理代码比较统一，所以 SpringAMQP 实现了消息确认，并允许通过配置文件设置 ACK 处理方式，有三种模式：

- none：不处理。即消息投递给消费者后立刻返回 ack，消息会立刻从 MQ 删除，非常不安全，不建议使用
- manual：手动模式。需要手动在业务代码中调用 api，发送 ack 或 reject，虽然存在业务入侵，但更灵活
- auto：自动模式。SpringAMQP 利用 AOP 对消息处理逻辑做了环绕增强，当业务正常执行时则自动返回 ack；当业务出现异常时，根据异常判断返回不同结果：
  - 如果是业务异常，会自动返回 nack
  - 如果是消息处理或校验异常，自动返回 reject

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: none # 不做处理，也是默认机制
```

修改 consumer 服务的 SpringRabbitListener 类中的方法，模拟一个消息处理的异常：

```java
@RabbitListener(queues = "simple.queue")
public void listenSimpleQueueMessage(String msg) throws InterruptedException {
    log.info("spring 消费者接收到消息：[{}]", msg);
    if (true) {
      throw new MessageConversionException("故意的");
    }
    log.info("消息处理完成");
}
```

当消息处理发生异常时，消息被 RabbitMQ 删除了，查看 RabbitMQ 控制台，点击获取消息，会显示队列为空。

```text
spring 消费者接收到消息：[hello, spring amqp!]
Caused by: org.springframework.amqp.support.converter.MessageConversionException: 故意的
```

****
#### 2. 失败重试机制






























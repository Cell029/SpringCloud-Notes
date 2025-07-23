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
### 4.2 自定义 SQL

在使用 UpdateWrapper 的时候，写了这样一段代码：

```java
UpdateWrapper<User> wrapper = new UpdateWrapper<User>()
            .setSql("balance = balance - 200") // SET balance = balance - 200
            .in("id", ids);
```

这种写法在某些企业也是不允许的，因为 SQL 语句最好都维护在持久层，而不是业务层。就当前案例来说，由于条件是 in 语句，只能将 SQL 写在 Mapper.xml 文件，
然后利用 foreach 来生成动态 SQL。但这实在是太麻烦了，假如查询条件更复杂，动态 SQL 的编写也会更加复杂。所以，MybatisPlus 提供了自定义 SQL 功能，
可以利用 Wrapper 生成查询条件，再结合 Mapper.xml 编写 SQL。

例如上述代码可以改写成在业务层通过 MyBatisPlus 定义一些复杂的查询条件（where in ...），然后把定义好的条件作为参数传递给 MyBatis 手写的 SQL，

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

然后在 Mapper 层手动拼接条件，需要注意的是，传递的参数必须通过 @Param 设置为 "ew" / Constants.WRAPPER，然后拼接时使用 ${} 拼接字符串的方式

```java
@Select("UPDATE user SET balance = balance - #{money} ${ew.customSqlSegment}")
void deductBalanceByIds(@Param("money") int money, @Param("ew") QueryWrapper<User> wrapper);
```

理论上来讲 MyBatisPlus 是不支持多表查询的，不过可以利用 Wrapper 中自定义条件结合自定义 SQL 来实现多表查询的效果。例如，
查询出所有收货地址在北京的并且用户 id 在 1、2、4 之中的用户，如果使用 MyBatis 是这样的：

```sql
<select id="queryUserByIdAndAddr" resultType="com.itheima.mp.domain.User">
    SELECT *
    FROM user u
    INNER JOIN address a ON u.id = a.user_id
    WHERE u.id
    <foreach collection="ids" separator="," item="id" open="IN (" close=")">
      #{id}
    </foreach>
    AND a.city = #{city}
</select>
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
List<User> queryUserByWrapper(@Param(Constants.WRAPPER)QueryWrapper<User> wrapper);
```

也可以在 UserMapper.xml 中写：

```sql
<select id="queryUserByIdAndAddr" resultType="com.itheima.mp.domain.User">
    SELECT * FROM user u INNER JOIN address a ON u.id = a.user_id ${ew.customSqlSegment}
</select>
```

****
### 4.3 Service 接口

MybatisPlus 不仅提供了 BaseMapper，还提供了通用的 Service 接口及默认实现，封装了一些常用的 service 模板方法。通用接口为 IService，默认实现为 ServiceImpl，
其中封装的方法可以分为以下几类：

- save：新增
- remove：删除
- update：更新
- get：查询单个结果
- list：查询集合结果
- count：计数
- page：分页查询

#### 1. 基本用法

由于 Service 中经常需要定义与业务有关的自定义方法，所以不能直接使用 IService，而是自定义一个 Service 接口，然后继承 IService 来拓展方法。同时，
让自定义的 Service 的实现类继承 ServiceImpl，这样就不用自己实现 IService 中的接口了。

```java
public interface IUserService extends IService<User> {
}

// 如果后续需要自定义方法，则需要实现 IUserService
public class UserServiceImpl extends ServiceImpl<UserMapper, User>{
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
public void saveUser(@RequestBody UserFormDTO userFormDTO){
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
public void removeUserById(@PathVariable("id") Long userId){
    userService.removeById(userId);
}
```

3、根据 id 查询用户

```java
@GetMapping("/{id}")
@Operation(summary = "根据id查询用户")
public UserVO queryUserById(@PathVariable("id") Long userId){
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
public List<UserVO> queryUserByIds(@RequestParam("ids") List<Long> ids){
    List<User> users = userService.listByIds(ids);
    return BeanUtil.copyToList(users, UserVO.class);
}
```

5、根据 id 扣减余额

上述接口都直接在 controller 即可实现，无需编写任何 service 代码，非常方便。但一些带有业务逻辑的接口则需要在 service 中自定义实现，例如此功能的修改操作就涉及：

- 判断用户状态是否正常
- 判断用户余额是否充足

这些业务逻辑都要在 service 层来做，另外更新余额需要自定义 SQL，要在 mapper 中来实现：

Controller 层：

```java
@PutMapping("{id}/deduction/{money}")
@Operation(summary = "扣减用户余额")
public void deductBalance(@PathVariable("id") Long id, @PathVariable("money")Integer money){
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
public List<UserVO> queryUsers(UserQuery query){
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

在组织查询条件的时候，加入了 username != null 这样的参数，意思就是当条件成立时才会添加这个查询条件，类似 mapper.xml 文件中的 `<if>` 标签，
这样就实现了动态查询条件效果了。但这种写法仍较为麻烦，所以 Service 中对 LambdaQueryWrapper 和 LambdaUpdateWrapper 的用法进一步做了简化，
无需通过 new 的方式来创建 Wrapper，而是直接调用 lambdaQuery 和 lambdaUpdate 方法：

```java
public List<UserVO> queryUsers(UserQuery query){
    ...
    List<User> users = userService.lambdaQuery()
            .like(username != null, User::getUsername, username)
            .eq(status != null, User::getStatus, status)
            .ge(minBalance != null, User::getBalance, minBalance)
            .le(maxBalance != null, User::getBalance, maxBalance)
            .list();
}
```

可以发现 lambdaQuery 方法中除了可以构建条件，还需要在链式编程的最后添加一个 list()，这是在告诉 MP 调用结果需要一个 list 集合，其它可选的方法有：

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

但上面 MyBatisPlus 提供的 saveBatch 批量插入操作，在底层仍然是将数据一条一条的插入数据库的，而效率最高的应该是将所有的数据全部封装进一个 SQL 语句，即只执行一次插入操作。
MySQL 的客户端连接参数中有这样的一个参数：rewriteBatchedStatements，它就是重写批处理的 statement 语句。这个参数的默认值是 false，将其配置为 true 即代表开启批处理模式，
开启后可以保证最终只执行 100 次插入。

****
## 5. 扩展功能

### 5.1 代码生成

在使用 MybatisPlus 以后，基础的 Mapper、Service、POJO 代码相对固定，重复编写也比较麻烦，所以 MybatisPlus 官方提供了代码生成器根据数据库表结构生成 POJO、Mapper、Service 等相关代码，
只不过代码生成器同样要编码使用。所以更推荐使用一款 MybatisPlus 的插件，它可以基于图形化界面完成 MybatisPlus 的代码生成。

****
### 5.2 静态工具

有的时候 Service 之间也会相互调用，为了避免出现循环依赖问题，MybatisPlus 提供一个静态工具类 Db，它本质上是一个对 BaseMapper 的静态代理，
内部通过 SpringContextUtils.getBean(Class) 动态获取对应实体类的 BaseMapper<T> 实例，然后再调用其方法，主要是用于简化数据库操作，
可以在没有手动注入 Service 或 Mapper 的前提下执行常用操作。

```java
Db.save(entity);
Db.updateById(entity);
Db.removeById(id);
Db.list(new QueryWrapper<>());
Db.getById(id);
// 等价于
userService.save(entity);
userService.updateById(entity);
userService.removeById(id);
userService.list(queryWrapper);
userService.getById(id);
```

****
### 5.3 逻辑删除

对于一些比较重要的数据，可以采用逻辑删除的方案，即：

- 在表中添加一个字段标记数据是否被删除
- 当删除数据时把标记置为 true
- 查询时过滤掉标记为 true 的数据

可是一旦采用了逻辑删除，所有的查询和删除逻辑都要跟着变化，非常麻烦。所以 MybatisPlus 就添加了对逻辑删除的支持，
在对应的表和实体类中添加对应的逻辑删除字段（只有MybatisPlus生成的SQL语句才支持自动的逻辑删除，自定义SQL需要自己手动处理逻辑删除），然后在 yaml 中配置逻辑删除字段：

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
UPDATE address SET deleted=1 WHERE id=? AND deleted=0
```

测试查询，发现查询语句多了个 deleted = 0 的条件：

```sql
SELECT id,user_id,province,city,town,mobile,street,contact,is_default,notes,deleted FROM address WHERE deleted=0
```

****
### 5.4 通用枚举

在当前的 User 类中有一个 status 字段，它用来表示用户当前的状态，但像这种字段一般应该定义为一个枚举，做业务判断的时候就可以直接基于枚举做比较。
但是目前的数据库采用的是 Integer 类型，对应的 POJO 中的 status 字段也是 Integer 类型，因此业务操作时就必须手动把枚举与 Integer 转换，非常麻烦。
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

要让 MybatisPlus 处理枚举与数据库类型自动转换，就必须告诉 MybatisPlus 枚举中的哪个字段的值是作为数据库值的，所以要使用它提供的 @EnumValue 来标记枚举属性：

```java
@EnumValue
private final int value;
```

在 yaml 文件中添加配置枚举处理器：

```java
mybatis-plus:
  configuration:
    default-enum-type-handler: com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler
```

需要注意的是，需要在 UserStatus 的 desc 字段上添加 @JsonValue 注解，它是用来指定 JSON 序列化时展示的字段，即用 desc 字段表示该枚举本身：

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
{"age": 20, "intro": "佛系青年", "gender": "male"}
```

但目前 User 实体类中却是 String 类型，所以现在读取 info 中的属性时就非常不方便，如果要方便获取，info 的类型最好是一个 Map 或者实体类，可如果把 info 改为对象类型，
在写入数据库时就需要手动转换为 String，读取数据库时又需要手动转换成对象，过程十分繁琐，所以 MybatisPlus 提供了很多特殊类型字段的类型处理器，解决特殊字段类型与数据库类型转换的问题。
例如处理 JSON 就可以使用 JacksonTypeHandler 处理器。

定义一个 [User
Info](./mp-demo/src/main/java/com/itheima/mp/domain/po/UserInfo.java) 实体类来与 info 字段的属性匹配，并修改 User 的 info 字段的类型。
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

在 3.5.9 版本中，MyBatisPlus 对组件做了拆分，比如分页功能依赖的 `jsqlparser` 被单独拆成了 `mybatis-plus-jsqlparser` 包。要想让分页功能跑起来，
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
SELECT id,username,password,phone,info,status,balance,create_time,update_time FROM user LIMIT ?,?
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
public PageDTO<UserVO> queryUsersPage(UserQuery query){
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

设置两个条件查询条件，当前端传递的用户名称和用户状态不为空时，则把它们作为分页查询的限制条件，而 .page(page) 里面的那个 page，是构建好的分页条件：

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

封装了一个 PageQuery 实体类，专门用来构建分页查询的条件，在这里完成页码以及每页数据条数的初始化，因为 MybatisPlus 提供了查询结果的排序，
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

之前有学过，最后的返回结果虽然是一个 List 集合，但集合内部是具体的对象，所以需要返回一个 VO 类型的对象专门展示在前端，所以定义了一个 PageDTO<V> 实体类，
它可以自定义返回的具体类型，所以在 Service 层中可以传入 UserVO 类，将 User 类拷贝给 UserVO：

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页结果")
public class PageDTO<V> {
    @Schema(description ="总条数")
    private Long total;
    @Schema(description ="总页数")
    private Long pages;
    @Schema(description ="返回的数据集合")
    private List<V> list;
    
    public static <V, P> PageDTO<V> empty(Page<P> p){
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
return PageDTO.of(records, user -> {
    // 1. 拷贝基础属性
    UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
    // 2. 处理特殊逻辑
    userVO.setUsername(userVO.getUsername().substring(0, userVO.getUsername().length() - 2) + "**");
    return userVO;
});
```

这里就是将 userName 的最后两个字符变为 **。

****






















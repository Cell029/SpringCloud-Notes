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
    - 容器内端口往往是由容器内的进程决定，例如 MySQL 进程默认端口是 3306，因此容器内端口就是 3306；而宿主机端口则可以任意指定，一般与容器内保持一致。
    - 格式： -p 宿主机端口:容器内端口，该命令就是将宿主机的 3306 映射到容器内的 3306 端口
- -e TZ=Asia/Shanghai : 配置容器内进程运行时的一些参数
    - 格式：-e KEY=VALUE，KEY 和 VALUE 都由容器内进程决定
    - 案例中，TZ=Asia/Shanghai 是设置时区；MYSQL_ROOT_PASSWORD=123 是设置 MySQL 的默认密码
- mysql : 设置镜像名称，Docker 会根据这个名字搜索并下载镜像
    - 格式：REPOSITORY:TAG，例如 mysql:8.0，其中 REPOSITORY 可以理解为镜像名，TAG 是版本号
    - 在未指定 TAG 的情况下，默认是最新版本，也就是 mysql:latest

执行命令后，Docker 就会自动搜索并下载 MySQL，然后会自动运行 MySQL。而且，这种安装方式不用考虑运行的操作系统环境，它不仅可以在 CentOS 系统这样安装，
在 Ubuntu 系统、macOS 系统、甚至是装了 WSL 的 Windows 下，都可以使用这条命令来安装 MySQL。如果是手动安装，就需要手动解决安装包不同、环境不同的、配置不同的问题。
因为 Docker 安装 MySQL 不是直接下载它，而是拉取一个镜像，该镜像中不仅包含了 MySQL 本身，还包含了运行所需要的环境、配置、系统级函数库。基于此，
它在运行时就有自己独立的环境，可以跨系统运行，也不需要手动配置环境，这种独立运行的隔离环境被称为容器。

Docker 官方提供了一个专门管理、存储镜像的网站，并对外开放了镜像上传、下载的权利：[https://hub.docker.com/](https://hub.docker.com/)。
DockerHub 网站是官方仓库，阿里云、华为云会提供一些第三方仓库，也可以自己搭建私有的镜像仓库。

****
## 2. Docker 基础

官方文档：[https://docs.docker.com/](https://docs.docker.com/)

### 2.1 常见命令

| 命令          | 说明                          | 文档地址                                                                            |
| ------------- | ----------------------------- |---------------------------------------------------------------------------------|
| docker pull   | 拉取镜像                      | [docker pull](https://docs.docker.com/engine/reference/commandline/pull/)       |
| docker push   | 推送镜像到DockerRegistry      | [docker push](https://docs.docker.com/engine/reference/commandline/push/)       |
| docker images | 查看本地镜像                  | [docker images](https://docs.docker.com/engine/reference/commandline/images/)   |
| docker rmi    | 删除本地镜像                  | [docker rmi](https://docs.docker.com/engine/reference/commandline/rmi/)         |
| docker run    | 创建并运行容器（不能重复创建）| [docker run](https://docs.docker.com/engine/reference/commandline/run/)         |
| docker stop   | 停止指定容器                  | [docker stop](https://docs.docker.com/engine/reference/commandline/stop/)       |
| docker start  | 启动指定容器                  | [docker start](https://docs.docker.com/engine/reference/commandline/start/)     |
| docker restart| 重新启动容器                  | [docker restart](https://docs.docker.com/engine/reference/commandline/restart/) |
| docker rm     | 删除指定容器                  | [docker rm](https://docs.docker.com/engine/reference/commandline/rm/)           |
| docker ps     | 查看容器                      | [docker ps](https://docs.docker.com/engine/reference/commandline/ps/)           |
| docker logs   | 查看容器运行日志              | [docker logs](https://docs.docker.com/engine/reference/commandline/logs/)       |
| docker exec   | 进入容器                      | [docker exec](https://docs.docker.com/engine/reference/commandline/exec/)                  |
| docker save   | 保存镜像到本地压缩文件        | [docker save](https://docs.docker.com/engine/reference/commandline/save/)       |
| docker load   | 加载本地压缩文件到镜像        | [docker load](https://docs.docker.com/engine/reference/commandline/load/)       |
| docker inspect| 查看容器详细信息              | [docker inspect](https://docs.docker.com/engine/reference/commandline/inspect/) | 

Docker 的核心命令可以划分为三个主要环节：镜像构建与管理、镜像仓库交互、容器生命周期管理：

通过 docker build 命令，可以基于 Dockerfile 构建出一个自定义的镜像，比如定制版的 Nginx 服务镜像。构建好的镜像可以使用 docker images 查看详细信息，
如镜像名、标签、大小等；如果不再使用某个镜像，可以使用 docker rmi 删除它。Docker 还提供了离线共享机制 docker save 和 docker load：
前者将镜像打包为 .tar 文件，后者可从文件中恢复出镜像，实现离线迁移。

关于镜像仓库交互操作，使用 docker pull 可以从远程镜像仓库（如 Docker Hub）拉取镜像到本地，比如拉取官方提供的 MySQL 镜像；docker push 则可以将本地构建好的镜像上传到仓库，这一过程类似源代码的版本管理。

通过 docker run，可以基于镜像创建并启动一个新的容器，例如使用 Nginx 镜像启动一个 Web 服务。运行中的容器可以通过 docker stop 停止，
再用 docker start 重新启动，或者直接使用 docker restart。docker ps 可以查看当前正在运行的容器列表，了解容器状态和端口映射等情况（加上 -a 则是查看所有）。
docker logs 则用于查看容器的运行日志；而 docker exec 可以进入容器内部执行命令，比如修改配置或检查进程。当容器不再使用时，可以用 docker rm 将其删除。

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
容器的本质是轻量级、快速启动、易于销毁的运行环境，这就意味着容器生命周期短，随时可能被销毁或替换，因此程序的数据（如 MySQL 的数据库文件）、配置（如 nginx.conf）、资源（如静态资源） 不能直接放在容器里。

数据卷是 Docker 主机上的一个目录或文件，它可以被挂载到容器中。与容器的可写层不同，数据卷的数据不会随着容器的删除而丢失，并且对数据卷的修改会立即生效。它主要有以下几个作用：

- 数据持久化：使用数据卷后，即使容器被删除，数据卷中的数据依然保留在主机上，下次启动新容器时可以继续使用。
- 数据共享：多个容器可以同时挂载同一个数据卷，实现数据的共享。例如一个 Web 应用容器和一个数据库容器共享存储用户上传文件的目录。
- 简化配置：可以将配置文件放在数据卷中，在不同的容器中挂载相同的数据卷，这样就可以快速复用相同的配置，而无需在每个容器中单独配置。

相关命令：

| 命令                | 说明             | 文档地址                                                                                          |
| ------------------- | ---------------- |-----------------------------------------------------------------------------------------------|
| docker volume create | 创建数据卷       | [docker volume create](https://docs.docker.com/engine/reference/commandline/volume_create/)   |
| docker volume ls     | 查看所有数据卷   | [docker volume ls](https://docs.docker.com/engine/reference/commandline/volume_ls/)           |
| docker volume rm     | 删除指定数据卷   | [docker volume rm](https://docs.docker.com/engine/reference/commandline/volume_rm/)           |
| docker volume inspect | 查看某个数据卷的详情 | [docker volume inspect](https://docs.docker.com/engine/reference/commandline/volume_inspect/) |
| docker volume prune  | 清除数据卷       | [docker volume prune](https://docs.docker.com/engine/reference/commandline/volume_prune/)                          | 

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

上述配置是将容器内的 /var/lib/mysql 这个目录，与数据卷 278e740c8... 挂载，于是在宿主机中就有了 /var/lib/docker/volumes/278e740c8... 这个目录。
这就是匿名数据卷对应的目录，它的使用方式与普通数据卷没有差别。即使没有显式挂载数据卷，Docker 也会自动挂载一个匿名数据卷。因为 MySQL 镜像的 Dockerfile 中定义了：

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

数据卷的目录结构较复杂，如果直接操作数据卷目录会不太方便。大多情况下，应该直接将容器目录与宿主机指定目录挂载，或者直接挂载到 Windows 磁盘中。
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

现在尝试将本地 Windows 目录挂载到容器内，并且使用对应的初始化 SQL 脚本和配置文件，官方文档：[mysql](https://hub.docker.com/_/mysql)。

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

- 在 Windows 上使用 WSL2 或 Docker Desktop 挂载 Windows 目录时，Linux 容器往往无法正确操作 Windows 文件系统的权限，导致类似 “Operation not permitted” 的错误。
- 所以建议避免直接挂载 Windows 目录到 MySQL 的数据目录，只把配置文件和初始化脚本挂载到 Windows 目录，数据目录使用 Docker 卷，让 Docker 维护数据存储在其内部虚拟文件系统里。
- WSL2 中挂载 Windows 盘时，路径要用 Linux 格式，比如 /mnt/d/...；上述命令的路径写法为：/mnt/d/docker_dataMountDirectory/mysql/conf:/etc/mysql/conf.d

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

基于以上操作，完成本地目录的挂在后，即使删除了容器，本地目录内的数据是不会丢失的，容器里 /var/lib/mysql 所有的文件操作，都会映射到宿主机的挂载路径上；
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
只需要将打包的过程，每一层要做的事情用固定的语法写下来，交给 Docker 去执行即可，而这种记录镜像结构的文件就称为 Dockerfile，它是一个包含了一系列命令的脚本，这些命令按照顺序执行并生成最终的镜像。
官方文档：[https://docs.docker.com/engine/reference/builder/](https://docs.docker.com/engine/reference/builder/)

常用命令：

| 指令        | 说明                                   | 示例                      |
| ----------- | -------------------------------------- | ------------------------- |
| FROM        | 指定基础镜像                           | FROM centos:6             |
| ENV         | 设置环境变量，可在后面指令使用         | ENV key value             |
| COPY        | 拷贝本地文件到镜像的指定目录           | COPY ./xx.jar /tmp/app.jar |
| RUN         | 执行Linux的shell命令，一般是安装过程的命令 | RUN yum install gcc       |
| EXPOSE      | 指定容器运行时监听的端口，是给镜像使用者看的 | EXPOSE 8080               |
| ENTRYPOINT  | 镜像中应用的启动命令，容器运行时调用   | ENTRYPOINT java -jar xx.jar |

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

上面创建了一个 Java 项目的容器，而 Java 项目往往需要访问其它各种中间件，例如 MySQL、Redis 等。而 Docker 默认为所有容器创建一个叫作 bridge 的默认网络（除非显式使用 --network）。
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

但是，容器的网络 IP 其实是一个虚拟的 IP，其值并不固定与某一个容器绑定，如果在开发时写死某个 IP，而在部署时很可能 MySQL 容器的 IP 会发生变化，连接会失败。常见 Docker 网络的命令：

| 命令                     | 说明                     | 文档地址                                                                                                  |
| ------------------------ | ------------------------ |-------------------------------------------------------------------------------------------------------|
| docker network create    | 创建一个网络             | [docker network create](https://docs.docker.com/engine/reference/commandline/network_create/)         |
| docker network ls        | 查看所有网络             | [docker network ls](https://docs.docker.com/engine/reference/commandline/network_ls/)                 |
| docker network rm        | 删除指定网络             | [docker network rm](https://docs.docker.com/engine/reference/commandline/network_rm/)                 |
| docker network prune     | 清除未使用的网络         | [docker network prune](https://docs.docker.com/engine/reference/commandline/network_prune/)           |
| docker network connect   | 使指定容器连接加入某网络 | [docker network connect](https://docs.docker.com/engine/reference/commandline/network_connect/)       |
| docker network disconnect | 使指定容器连接离开某网络 | [docker network disconnect](https://docs.docker.com/engine/reference/commandline/network_disconnect/) |
| docker network inspect   | 查看网络详细信息         | [docker network inspect](https://docs.docker.com/engine/reference/commandline/network_inspect/)       | 

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

hmall 项目是一个 maven 聚合项目，使用 IDEA 打开 hmall 项目，它有两个子模块：一个 hm-common，一个 hm-service，需要进行部署的就是 hm-service；
因为 hm-common 模块本身不包含业务逻辑，也没有启动类，不能独立运行，在每个服务模块引入了 common 依赖，意味着在 maven 编译打包服务模块时，
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

但实际项目中不止这些，所以使用 Docker Compose 可以帮助实现多个相互关联的 Docker 容器的快速部署，它允许用户通过一个单独的 docker-compose.yml 模板文件（YAML 格式）来定义一组相关联的应用容器。

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

| docker run 参数 | docker compose 指令 | 说明     |
| :-------------: | :-----------------: | -------- |
|     --name      |    container_name   | 容器名称 |
|       -p        |        ports        | 端口映射 |
|       -e        |      environment    | 环境变量 |
|       -v        |       volumes       | 数据卷配置 |
|    --network    |       networks      | 网络     |

****
#### 3.2 基础命令

```shell
docker compose [OPTIONS] [COMMAND]
```

其中，OPTIONS 和 COMMAND 都是可选参数，比较常见的有：

| 类型      | 参数或指令 | 说明                                                         |
| --------- | ---------- | ------------------------------------------------------------ |
| Options   | -f         | 指定compose文件的路径和名称                                  |
| Options   | -p         | 指定project名称。project就是当前compose文件中设置的多个service的集合，是逻辑概念 |
| Commands  | up         | 创建并启动所有service容器                                    |
| Commands  | down       | 停止并移除所有容器、网络                                     |
| Commands  | ps         | 列出所有启动的容器                                           |
| Commands  | logs       | 查看指定容器的日志                                           |
| Commands  | stop       | 停止容器                                                     |
| Commands  | start      | 启动容器                                                     |
| Commands  | restart    | 重启容器                                                     |
| Commands  | top        | 查看运行的进程                                               |
| Commands  | exec       | 在指定的运行中容器中执行命令                                 |

****
# 三、微服务

## 1. 概述

### 1.1 单体架构

单体架构（monolithic structure）就是整个项目中所有功能模块都在一个工程中开发；项目部署时需要对所有模块一起编译、打包；项目的架构设计、开发模式都非常简单。
当项目规模较小时，这种模式上手快，部署、运维也都很方便，因此早期很多小型项目都采用这种模式。但随着项目的业务规模越来越大，团队开发人员也不断增加，单体架构就呈现出越来越多的问题：

- 团队协作成本高：由于所有模块都在一个项目中，不同模块的代码之间物理边界越来越模糊，最终要把功能合并到一个分支，此时可能发生各种 bug，导致解决问题较为麻烦。
- 系统发布效率低：任何模块变更都需要发布整个系统，而系统发布过程中需要多个模块之间制约较多，需要对比各种文件，任何一处出现问题都会导致发布失败，往往一次发布需要数十分钟甚至数小时。
- 系统可用性差：单体架构各个功能模块是作为一个服务部署，相互之间会互相影响，一些热点功能会耗尽系统资源，导致其它服务低可用。

例如访问下面两个接口：

- http://localhost:8080/hi
- http://localhost:8080/search/list

经过测试，目前 /search/list 是比较正常的，访问耗时在 30 毫秒左右。但如果此时 /hi 接口称为一个并发较高的热点接口，他就会抢占大量资源，最终会有越来越多请求积压，直至Tomcat资源耗尽。
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

在 hmall 父工程之中已经提前定义了 SpringBoot、SpringCloud 的依赖版本，所以可以直接在这个项目中创建微服务 module。购物车对应 cart-service，商品服务对应 item-service。
分别导入 controller、service 和 mapper。

****
### 2.3 服务调用

在拆分的时候有一个问题：就是购物车业务中需要查询商品信息，但商品信息查询的逻辑全部迁移到了 item-service 服务，导致无法查询。最终结果就是查询到的购物车数据不完整，
要解决这个问题就必须改造其中的代码，把原本本地方法调用，改造成跨微服务的远程调用（RPC，即 Remote Produce Call）。即修改以下的代码：

```java
// 1. 获取商品id
Set<Long> itemIds = vos.stream().map(CartVO::getItemId).collect(Collectors.toSet());
// 2. 查询商品
List<ItemDTO> items = itemService.queryItemByIds(itemIds);
```

当前端向服务端发送查询数据请求时，其实就是从浏览器远程查询服务端数据。比如通过 Swagger 测试商品查询接口，就是向 http://localhost:8081/items 这个接口发起的请求。
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

修改 cart-service 中的 com.hmall.cart.service.impl.CartServiceImpl 的 handleCartItems 方法，发送 http 请求到 item-service：

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
    if(!response.getStatusCode().is2xxSuccessful()){
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

需要注意的是，需要使用 RestTemplate 就需要把它注入进来，但 SpringBoot 推荐使用构造方法的方式进行注入，为了避免手写构造方法，所以可以使用 @RequiredArgsConstructor 注解，
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

基于 Docker 来部署 Nacos 的注册中心，要准备 MySQL 数据库表用来存储 Nacos 的数据。由于是 Docker 部署，所以需要将 SQL 文件导入到 Docker 中的 MySQL 容器中。
然后将 nacos/custom.env 文件上传至虚拟机：

```text
PREFER_HOST_MODE=hostname
MODE=standalone
SPRING_DATASOURCE_PLATFORM=mysql
MYSQL_SERVICE_HOST=192.168.0.105
MYSQL_SERVICE_DB_NAME=nacos
MYSQL_SERVICE_PORT=3306
MYSQL_SERVICE_USER=root
MYSQL_SERVICE_PASSWORD=123
MYSQL_SERVICE_DB_PARAM=characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
```

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

| 服务名       | 分组名称       | 集群数目 | 实例数 | 健康实例数 | 触发保护阈值 | 操作                             |
| ------------ | -------------- | -------- | ------ | ---------- | ------------ | -------------------------------- |
| cart-service | DEFAULT_GROUP  | 1        | 1      | 1          | false        | 详情 \| 示例代码 \| 订阅者 \| 删除 |
| item-service | DEFAULT_GROUP  | 1        | 1      | 1          | false        | 详情 \| 示例代码 \| 订阅者 \| 删除 |

然后服务调用者 cart-service 就可以去订阅 item-service 服务了，不过 item-service 可能有多个实例，而真正发起调用时只需要知道一个实例的地址。所以服务调用者必须利用负载均衡从多个实例中挑选一个去访问。
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

上面利用 Nacos 实现了服务的治理，利用 RestTemplate 实现了服务的远程调用，但是远程调用的代码太复杂了，一会儿远程调用，一会儿本地调用。所以引出了 OpenFeign 组件。
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

配置了上述信息，OpenFeign 就可以利用动态代理实现该方法，并且向 http://item-service/items 发送一个 GET 请求，携带 ids 为请求参数，并自动将返回值处理为 List<ItemDTO>。
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

Feign 是一个声明式的 HTTP 客户端，其底层真正发起 HTTP 请求时，是依赖第三方的 HTTP 客户端库来完成的。其底层支持的 http 客户端实现包括：

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

在拆分 item-service 和 cart-service 两个微服务时，它们里面有部分代码是与需求是一样的，如果此时还要拆分一个 trade-service，它也需要远程调用 item-service 中的根据 id 批量查询商品，
这个功能与 cart-service 中是一样的，所以为了避免大量编写重复的代码，就需要提取它们的公共部分，例如：

- 方案1：抽取到微服务之外的公共 module（即作为一个新的微服务）
- 方案2：每个微服务自己抽取一个 module（即作为微服务的子模块）

方案1抽取更加简单，工程结构也比较清晰，但缺点是整个项目耦合度偏高；方案2抽取相对麻烦，工程结构相对更复杂，但服务之间耦合度降低。在 hmall 下定义一个新的 module，
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

然后把需要重复使用到的 ItemDTO 和 ItemClient 都放到该模块下，其它需要使用到远程调用功能的地方直接导入 hm-api 包即可，不过需要引入 hm-api 作为依赖：

```xml
<!--feign模块-->
<dependency>
    <groupId>com.heima</groupId>
    <artifactId>hm-api</artifactId>
    <version>1.0.0</version>
</dependency>
```

需要注意的是:因为 ItemClient 现在定义到了 com.hmall.api.client 包下，而 cart-service 的启动类定义在 com.hmall.cart 包下，这就导致扫描不到 ItemClient，
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
    public Logger.Level feignLogLevel(){
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
# 三、网关路由

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

| 名称      | 说明                         | 示例                                                         |
| --------- | ---------------------------- | ------------------------------------------------------------ |
| After     | 是某个时间点后的请求         | - After=2037-01-20T17:42:47.789-07:00[America/Denver]        |
| Before    | 是某个时间点之前的请求       | - Before=2031-04-13T15:14:47.433+08:00[Asia/Shanghai]       |
| Between   | 是某两个时间点之前的请求     | - Between=2037-01-20T17:42:47.789-07:00[America/Denver], 2037-01-21T17:42:47.789-07:00[America/Denver] |
| Cookie    | 请求必须包含某些cookie       | - Cookie=chocolate, ch.p                                     |
| Header    | 请求必须包含某些header       | - Header=X-Request-Id, \d+                                   |
| Host      | 请求必须是访问某个host（域名） | - Host=**.somehost.org,**.anotherhost.org                    |
| Method    | 请求方式必须是指定方式       | - Method=GET,POST                                            |
| Path      | 请求路径必须符合指定规则     | - Path=/red/{segment},/blue/**                               |
| Query     | 请求参数必须包含指定参数     | - Query=name, Jack或者- Query=name                          |
| RemoteAddr | 请求者的ip必须是指定范围     | - RemoteAddr=192.168.1.1/24                                  |
| weight    | 权重处理                     |                                                              |

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

- GatewayFilter：路由过滤器，作用范围比较灵活，可以是任意指定的路由 Route，绑定到某条路由规则上才会生效。比如 /pay-orders/** 路由的请求才需要做特殊的签名校验，那就只为这个 Route 配置一个 GatewayFilter
- GlobalFilter：全局过滤器，作用范围是所有路由，声明后自动生效。在代码中通过实现 GlobalFilter 接口并注册为 Spring Bean 后做统一的校验，或者使用 default-filters

常用 Gateway 中内置的 GatewayFilter 过滤器：

| 过滤器名称                  | 作用说明                           |
| ---------------------- | ------------------------------ |
| `AddRequestHeader`     | 添加请求头                          |
| `AddResponseHeader`    | 添加响应头                          |
| `RemoveRequestHeader`  | 移除请求头                          |
| `RemoveResponseHeader` | 移除响应头                          |
| `RewritePath`          | 重写请求路径                         |
| `SetStatus`            | 设置返回状态码                        |
| `RedirectTo`           | 重定向                            |

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

自定义 GatewayFilter 不是直接实现 GatewayFilter，而是实现 AbstractGatewayFilterFactory，且该类的名称一定要以 GatewayFilterFactory 为后缀：

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

可以直接返回一个 GatewayFilter 过滤器内部类，也可以使用 OrderedGatewayFilter 指定优先级（因为内部类不能实现接口），然后在 yaml 配置中这样使用：

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
    static class Config{
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
if(isExclude(request.getPath().toString())){
  // 无需拦截，直接放行
  return chain.filter(exchange);
}

private boolean isExclude(String antPath) {
    for (String pathPattern : authProperties.getExcludePaths()) {
        if(antPathMatcher.match(pathPattern, antPath)){
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
        if(isExclude(request.getPath().toString())){
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
            if(antPathMatcher.match(pathPattern, antPath)){
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
考虑到微服务内部可能很多地方都需要用到登录用户信息，因此可以利用 SpringMVC 的拦截器来实现登录用户信息获取，并存入 ThreadLocal，方便后续使用。

#### 1. 保存用户到请求头

ServerWebExchange 是 WebFlux 中的请求上下文对象，包含了请求 ServerHttpRequest 和响应 ServerHttpResponse，它是不可变的，如果要修改 request，
比如添加请求头，就得调用 exchange.mutate() 来创建一个新的、可修改的构建器。然后放行时不再使用 exchange 上下文对象，而是使用修改了 request 的上下文对象。
而 builder 是 ServerHttpRequest.Builder 对象，它是用来构建修改后的 ServerHttpRequest。`.header("user-info", userInfo)` 表示在原始请求的基础上，
添加一个名为 user-info 的请求头，值是用户 ID（转成字符串）。需要注意的是：header() 方法是追加，不会覆盖已有的同名 header（如果存在多个值，会变成列表）

```java
// 5. 如果有效，传递用户信息
System.out.println("userId = " + userId);
String userInfo = userId.toString();
ServerWebExchange swe = exchange.mutate()
        .request(builder -> builder.header("user-info", userInfo))
        .build();
// 6. 放行
return chain.filter(swe);
```

在微服务架构中，一般只有网关会直接接触到客户端发来的 JWT token。下游服务（如 user-service, trade-service）通常不再自己解析 token，而是依赖网关，
网关通过请求头 header（如 "user-info"）传递给下游服务。

****
#### 2. 拦截器获取用户

由于每个微服务都有获取登录用户的需求，因此拦截器可以直接写在 hm-common 中，并写好自动装配，这样微服务只需要引入 hm-common 就可以直接具备拦截器功能，无需重复编写。

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

需要注意的是：这个配置类默认是不会生效的，因为它所在的包是 com.hmall.common.config，与其它微服务的扫描包不一致，无法被扫描到，因此无法生效。但基于 SpringBoot 的自动装配原理，
只要将其添加到 resources 目录下的 META-INF/spring.factories 文件中即可被扫描到：

```factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.hmall.common.config.MyBatisConfig,\
  com.hmall.common.config.MvcConfig,\
  com.hmall.common.config.JsonConfig
```

即告诉 Spring Boot：当项目引入了这个模块（hm-common）时，请自动加载 MvcConfig 这个类中的配置。但在 Springboot 3.x 版本后就不再使用这种方式了，
而是使用 org.springframework.boot.autoconfigure.AutoConfiguration.imports，即在 resources 目录下的 META-INF/spring/ 创建 org.springframework.boot.autoconfigure.AutoConfiguration.imports 文件，
然后在文件中添加：

```text
com.hmall.common.config.MyBatisConfig
com.hmall.common.config.MvcConfig
com.hmall.common.config.JsonConfig
```

还有一点需要注意：Spring Cloud Gateway 是基于 Spring WebFlux 的响应式编程模型构建的，而 Spring MVC 是基于 Servlet API 的阻塞式编程模型，
两者不能同时存在于同一个应用程序中。而配置的拦截器 WebMvcConfig 是属于 Spring MVC 的，但网关模块中也引入了该包，所以启动时必定会报错，
所以需要使用到一个注解：@ConditionalOnClass，它的作用就是当条件生效时该类才加载，所以可以使用 @ConditionalOnClass(DispatcherServlet.class)，
因为微服务使用的是 SpringMVC，那就一定有这个转发请求的类存在，而网关中一定没有，所以网关模块中就不会加载 SpringMVC 的配置，从而避免发生报错。

****
### 2.5 OpenFeign 传递用户

前端发起的请求都会经过网关再到微服务，搭配过滤器和拦截器微服务可以获取登录用户信息。但是有些业务会在微服务之间调用其它微服务，也就是说这些方法的调用不会经过网关，
那么也就无法获取到存放在请求头中的 userInfo。例如：下单的过程中，需要调用商品服务扣减库存，即调用购物车服务清理用户购物车，而清理购物车时必须知道当前登录的用户身份。
但是，订单服务调用购物车时并没有传递用户信息，购物车服务无法知道当前用户是谁，即 SQL 中的 where userId = ? 为 null，执行肯定失败。而微服务之间调用是基于 OpenFeign 来实现的，
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

只需要实现这个接口并重写 apply 方法，利用 RequestTemplate 类来添加请求头，将用户信息保存到请求头中，每次 OpenFeign 发起请求的时候都会调用该方法，传递用户信息。
由于 FeignClient 全部都是在 hm-api 模块，所以直接在 hm-api 模块的 com.hmall.api.config.DefaultFeignConfig 中编写这个拦截器：

```java
@Bean
public RequestInterceptor userInfoRequestInterceptor(){
    return new RequestInterceptor() {
        @Override
        public void apply(RequestTemplate template) {
            // 获取登录用户
            Long userId = UserContext.getUser();
            if(userId == null) {
                // 如果为空则直接跳过
                return;
            }
            // 如果不为空则放入请求头中，传递给下游微服务
            template.header("user-info", userId.toString());
        }
    };
}
```

RequestTemplate 就是用于组装请求信息的工具，这个 template.header(...) 就是给 OpenFeign 的这次请求添加一个请求头，底层会在实际发送请求时将添加的所有信息变成真实的 HTTP 请求。
因为在 Controller 请求入口时，通过 Spring 拦截器就提取请求头中的用户信息，也就是一经过网关，用户信息就被读出并存在 ThreadLocal 了，在调用 Feign 请求前，
就从 ThreadLocal 中拿出用户信息，主动添加到请求头中，转发给下一个微服务。

****



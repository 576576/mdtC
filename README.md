# MdtC
Version: `1.2`

本转换器用于将类Java代码转为mindustry处理器代码.  
输入: 由类java语言(aka.`mdtc`)编写,`.mdtc`后缀文件.  
输出: 用于Mindustry处理器的语句输出到`.mdtcode`.

### 命令行参数

- `-v, --version`: 显示版本信息
- `-f, --format`: 格式化代码
- `-fo, --format-only`: 仅格式化代码
- `-i, --file <文件路径>`: 指定文件路径
- `-o, --output <输出路径>`: 指定输出路径
- `-oo, --open-out`: 编译后打开输出
- `-gpc, --generate-prime-code <代码等级>`: 产生中间代码(硬链接前), 1接近ts, 2接近汇编

### *查看这些示例快速开始*
- [钍堆防爆](sample_cases/failsafe_钍堆.mdtc)
- [采矿逻辑-5单位max](sample_cases/mine%20u5.mdtc)


### TodoList
- 完善代码检查/自动补全
- 编写为游戏模组
---

## 功能

### 0.注释与标签
```githubexpressionlanguage
::这是注释,也是标签
tag(这也是标签,而且是全局标签)

::代码头尾自带HEAD和END标签
::缺省时代码头自带DEFAULT标签
```
输出: [`case0.mdtcode`](./sample_cases/case0.mdtcode)


### 1. 赋值与计算
```githubexpressionlanguage
::赋值
x0="Hello World"

::计算
x=1+(-1)-(-x0)*4/5//6%7%%8.^9
y=1==(2!=3)&&4<5<=6>7>=8===9
z=1<<2>>3>>>4|5&6^7

i=max(min(len(1,2),noise(3,4)),angle(log(5,6),angleDiff(7,8)))
j=not(abs(sign(ln(lg(lb(floor(ceil(7.8))))))))
k=round(sqrt(rand(sin(cos(tan(asin(acos(atan(9)))))))))
```
输出: [`case1.mdtcode`](./sample_cases/case1.mdtcode)


### 2. 控制(无副作用)
```githubexpressionlanguage
::控制(无副作用)
wait(6)
stop()
end()

print("1145{0}")
printchar(64)
format(14)
ubind(@mono)
uctrl(payDrop)
ushoot(1).target(114,514)
ushoot(i).target(ene1)
raw("end")

::点控制(无副作用)
b1.enable(false).config(null).color(0)
b2.shoot().target(x,y)
b2.shoot(st).target(u1).config(@copper)
c.ulocate(turret).enemy(1)
msg.pflush().dflush()
color1.unpack(r,g,b,a)
result.write(cell1,x1)

::拓展
printf("666{0}yf{1}",6,cr1)
```
```githubexpressionlanguage
::拓展
printf("666{0}yf{1}",6,cr1)
```
输出: [`case2.mdtcode`](./sample_cases/case2.mdtcode)


### 3. 控制(有副作用)
```githubexpressionlanguage
::控制(有副作用)
r=link(i)
g=block(unit(item(liquid(team(LookupIndex)))))*pack(r,g,b,a)
b=uradar()===radar(t5).target(player,ground).order(0).sort(maxHealth)

::点控制(有副作用)
x=b1.sensor(@lead)+c1.read(bit2)
y=a.orElse(b).when(x!=0).orElse(c).when(r)
```
输出: [`case3.mdtcode`](./sample_cases/case3.mdtcode)


### 4. 跳转/分支/循环
```githubexpressionlanguage
::原生跳转jump
::不指定标签时跳转到DEFAULT
::不指定条件时始终跳转
::tag1
e=e+1
jump(tag1).when(e<6)

::分支if-else
if(init==0){
	mid=rand(100000)
	u.flag=floor(mid)
	init=1
}
else{
    end()
}

::循环for
::例:钍堆防爆
for(e=0;e<@links;e=e+1){
	t=link(e)
	case= t.sensor(@heat)<0.01 && t.sensor(@thorium)>27
	t.enable(case)
}

::循环do-while
do{
	e=e-1
}while(e.^3>(-114514))

::跳转jump2
::警告:jump2的未定义行为不会报错,必须手动检查标签
jump2(+4)
jump2(+min(19198,1).^0)
jump2(0)
```
输出: [`case4.mdtcode`](./sample_cases/case4.mdtcode)


### 5. 绘图
```githubexpressionlanguage
::仅简单参数传递
draw(clear)
draw(image,0,0,@copper,32,0,0)
```


### 6. 函数
函数体应定义于主代码后, 分为有副作用和无副作用两种  
定义时应指定输入参数和返回值名(void为无返回值)  
调用时使用funcName(funcArgs)调用
```githubexpressionlanguage
::函数
::示例使用
println("Hello world")
message1.pflush()
for(e=0;e<@links;e=e+1){
	t=link(e)
	t.enable(isReactorSafe(t))
}
onebind(u,@mono,114514)


::示例定义(无副作用)
::例:println
function void println(str){
	print(str)
	print("\n")
}

::示例定义(有副作用)
::例:钍堆安全
function status isReactorSafe(th_reactor){
	status= th_reactor.sensor(@heat)<0.01 && th_reactor.sensor(@thorium)>27
}

::示例定义(带内部标签)
::例:单位绑定控制
::绑定一个类型u.type单位, 保存到u, 标记u.fx; (unsafe)跳转到bind.end
function void onebind(u,u.type,u.fx){
	if(u==null){
		::init
		ubind(u.type)
		u=@unit
	}
	ubind(u)
	jump(init).when(u.sensor(@dead)!=0)
	u.cer=u.sensor(@controller)
	if(u.cer!=@unit){
		jump(init).when(u.cer!=@this)
		jump(init).when(u.sensor(@flag)!=u.fx)
	}
	uctrl(flag,u.fx)
	jump2(bind.end)
}
```
输出: [`case5.mdtcode`](./sample_cases/case5.mdtcode)


### 7. 重复和导入
repeat对代码段多次重复
等价的1D数组实现, 嵌套即可实现n维数组
```githubexpressionlanguage
::repeat和import使用示例
import sample_cases/modules/example

repeat(flag,3){
	flag=simpleFlag(5)
}
```
库文件: [`example.libmdtc`](./sample_cases/modules/example.libmdtc)  
输出: [`case6.mdtcode`](./sample_cases/case6.mdtcode) 

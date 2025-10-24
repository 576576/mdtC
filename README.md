# MdtC
Version: `1.0`

本转换器用于将类Java代码转为mindustry处理器代码.  
输入: 由类java语言(aka.`mdtc`)编写,`.mdtc`后缀文件.  
输出: 用于Mindustry处理器的语句输出到`.mdtcode`.

### *查看这些示例快速开始*
- [钍堆防爆](sample_cases/failsafe_钍堆.mdtc)
- [采矿逻辑-5单位max](sample_cases/mine%20u5.mdtc)


### TodoList
- 完善代码检查/自动补全
- 支持模块导入
- 附件为游戏模组

> 初始版本README见[这里](./readme_original.txt)
---

## 功能

### 0.注释和标签
```githubexpressionlanguage
::这是注释,也是标签
::jump必须绑定标签,eg:
::jump(这是注释,也是标签)
::没有指定标签的jump会跳到DEFAULT标签
::代码头尾自带HEAD和END标签
```

### 1. 赋值
```githubexpressionlanguage
x0="Hello World"
```

输出: `set x0 "Hello World"`


### 2. 计算
```githubexpressionlanguage
::一般
q=1+1
x=1+2-3*4/5//6%7%%8.^9
y=1==2!=3&&4<5<=6>7>=8===9
z=1<<2>>3>>>4|5&6^7
i=max(1,2)+min(3,4)+angle(5,6)+angleDiff(7,8)
j=len(1,2)+noise(3,4)+log(5,6)
k=not(1)*abs(2)*sign(3)*ln(4)*lg(5)*floor(6.7)*ceil(8.9)

::混合
res=re-1+isd2*abs(c)^(min(3,x3)+8)*yy5
```
输出: [`case1.mdtcode`](./sample_cases/case1.mdtcode)


### 3. 控制(无副作用)
```githubexpressionlanguage
::控制(无副作用)
print("1145{0}")
printchar(64)
format(14)
wait(6)
ubind(@mono)
stop()
end()

::点控制(无副作用)
b1.enable(false).config(null).color(0)
b2.shoot().target(x,y)
b2.shoot(st).target(u1).config(@copper)
c.ulocate(turret).enemy(1)
msg.pflush().dflush()
color1.unpack(r,g,b,a)
result.write(cell1,x1)
```
```githubexpressionlanguage
::拓展
printf("666{0}yf{1}",6,cr1)
```
输出: [`case2.mdtcode`](./sample_cases/case2.mdtcode)


### 4. 控制(有副作用)
```githubexpressionlanguage
::控制(有副作用)
s=link(i)
g=block(2)*unit(3)*item(4)*liquid(5)*team(6)*pack(r,g,b,a)
m=uradar()==radar(t5).target(player,ground).order(0).sort(maxHealth)

::点控制(有副作用)
res=b1.sensor(@lead)+c1.read(bit2)
```
输出: [`case3.mdtcode`](./sample_cases/case3.mdtcode)


### 5. 跳转
```githubexpressionlanguage
::例:钍堆防爆
::func1
t=link(e)
case= (t.sensor(@heat)<0.01)&&(t.sensor(@thorium)>27)
t.enable(case)
e=e+1
jump(func1).when(e<@links)
e=0
```
```githubexpressionlanguage
::拓展
::跳转(@counter)
jump2(+4)
jump2(+max(t,5).^3-625)
jump2(6)

::分支
::例：初始化
if(init==0){
mid=rand(100000)
u.flag=floor(mid)
init=1
}

::for循环
for(e=0;e<@links;e=e+1){
t=link(e)
case=(t.sensor(@heat)<0.01)&&(t.sensor(@thorium)>27)
t.enable(case)
}

::do-while循环
e=0
do{
t=link(e)
case=(t.sensor(@heat)<0.01)&&(t.sensor(@thorium)>27)
t.enable(case)
e=e+1
}while(e<@links)
```
输出: [`case4.mdtcode`](./sample_cases/case4.mdtcode)


### 6. 绘图
```githubexpressionlanguage
::仅简单参数传递
draw(clear)
draw(image,0,0,@copper,32,0,0)
```


### 7. 函数
`@todo`  
函数体应定义于主代码后, 分为有副作用和无副作用两种  
定义时应指定输入参数和返回值名(void为无返回值)  
调用时使用funcName(funcArgs)调用
```githubexpressionlanguage
::示例定义(无副作用)
::例:单位绑定控制
function void onebind(u){
::init
ubind(u.type)
u=@unit

ubind(u)
jump(init).when(u.sensor(@dead)!=0)
u.cer=u.sensor(@controller)
jump(break).when(u.cer==@unit)
jump(init).when(u.cer!=@this)
jump(init).when(u.sensor(@flag)!=u.fx)

::break
uctrl(flag,u.fx)
jump2(bind.end)
}


::示例定义(有副作用)
::例:钍堆安全
function status isReactorSafe(th_reactor){
status=(th_reactor.sensor(@heat)<0.01)&&(th_reactor.sensor(@thorium)>27)
}
```
输出: [`case5.mdtcode`](./sample_cases/case5.mdtcode)


### 8. 重复
对代码段多次重复
```githubexpressionlanguage
::repeat使用示例
repeat(u,3){
	u=link(e)
	e=e+1
}
str=strln3("sayounara")


function str strln3(str){
	repeat(3){
		str=str+"\n"
	}
}
```
输出: [`case6.mdtcode`](./sample_cases/case6.mdtcode)

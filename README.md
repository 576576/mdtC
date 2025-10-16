# MdtC

## 说明

本转换器用于将类c代码转为mindustry处理器代码.  
本转换器(也被称作`MdtC Compiler`)输入类表达式语言编写的`.mdtc`文件,并将其转换为适用于Mindustry处理器的语句输出到`.mdtcode`.

### TodoList
函数体预定义
> 初始版本详见[这里](./readme_original.txt)

---

## 功能
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
`@todo`

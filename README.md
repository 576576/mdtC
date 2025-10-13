# Mindustry代码转换器 文档

## 说明

本仓库用于将文本编辑器代码转为mindustry处理器代码.  
本转换器(也被称作`MdtC Compiler`)输入类表达式语言编写的`.mdtc`文件,并将其转换为适用于Mindustry处理器的语句输出到`.mdtcode`.

### TodoList  
for/while  
函数体预定义

> 初始版本详见[这里](./readme_original.txt)

---

## a. 原版功能
### 1. 赋值
```githubexpressionlanguage
x0="Hello World"
```

输出: `set x0 "Hello World"`


### 2. 计算
```githubexpressionlanguage
::简单计算
::...

::混合计算
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
输出: [`case2.mdtcode`](./sample_cases/case2.mdtcode)


### 4. 控制(有副作用)
```githubexpressionlanguage
::控制(有副作用)
s=link(i)
g=block(2)*unit(3)*item(4)*liquid(5)*team(6)*pack(r,g,b,a)
m=uradar()==radar(t5).target(player,ground).order(0).sort(maxHealth)


::点控制(有副作用)
res=b1.sensor(@copper)+c1.read(bit2)
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
输出: [`case4.mdtcode`](./sample_cases/case4.mdtcode)


### 6. 绘图
`@todo`

---

## b. 拓展功能
`@todo`

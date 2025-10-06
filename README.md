mindustry代码转换器 文档

说明: 以下涉及代码示例的部分分两行,前一行为输入代码,后一行或多行为转换的结果代码,参数计数从0开始

a. 逻辑操作logic

1.简单赋值set
参数数量2

result=p1
set result p1


2.运算op
参数数量2-3
运算以函数方式输入
该类运算函数类型:{
参数2:{
not abs sign log log10 floor ceil round sqrt rand sin cos tan asin acos atan
}

参数3(中间):{
add sub mul div idiv mod emod pow equal notEqual land lessThan lessThanEq greaterThan greaterThanEq strictEqual shl shr ushr or and xor
}

参数3(前置):{
max min angle angleDiff len noise logn
}

}


对于参数2,
生成结果代码的第二个参数始终为0
特别的,将ln转为log,将lg转为log10

result=abs(p1)
op abs result p1 0

result=ln(p1)
op log result p1 0

result=lg(p1)
op log10 result p1 0

对于参数3(中间),
按顺序解析符号,若连续使用,则应按照括号优先,加减最后的规律解析成多行代码,中间变量命名mid,若有多个则从mid.1开始,按照一一对应原则转换以下符号

{+ - * / // % %% ^ == != & < <= > >= === << >> >>> | && ^^}

result=p1^^p2
op xor result p1 p2

result=p1-p2/p3
op div mid p2 p3

result=p1+p2*(p3//p4)
op idiv mid.1 p3 p4
op mul mid.2 p2 mid.1
op add result p1 mid.2

对于参数3(前置),
特别的,将log转为logn并调转参数1和参数2顺序

result=max(p1,p2)
op max result p1 p2

result=log(p1,p2)
op logn result p2 p1


3.简单分支赋值select
分支运算符为三元选择运算符?:
条件为参数1-2,结果为参数3-4
特别的,对于always型,优化为简单赋值,always型永远不会从输入代码中生成为输出代码

运算符的类型:
{equal notEqual lessThan lessThanEq greaterThan greaterThanEq strictEqual always}
在输入代码中为:
{== != < <= > >= === \/}

result=(p1==p2)?p3:p4
select result equal p1 p2 p3 p4

result=(p1\/p2)?p3:p4
set result p3


4.查找转换lookup
lookup将id转化为对应的对象类型

该类函数类型:
{block unit item liquid team}

result=item(p1)
lookup item result p1

5.打包颜色和解包
颜色的rgba值范围为0-1

result=pcolor(r,g,b,a)
packcolor result r g b a

upcolor(r,g,b,a)=result
unpackcolor r g b a result

b.标准输入输出io

1. 内存io
采用链式函数来输入参数
read后连接from,分别输入1个参数
write接收2个参数,链式连接在要写入的内容后

result=read(p1).from(cell1)
read result cell1 p1

result.write(cell1,p1)
write result cell1 p1

2.绘画io
略,等待施工

3.信息板io
3种简单信息板io,外加一个printf
对于printf,解析为print和format的结合,预期输入参数为带有占位符的字符串(占位符用大括号{}表示),解析时将{}内依次替换为从0开始递增的数字并增加一条参数为大括号{}内原内容的format指令

print(p1)
print p1

printchar(65)
printchar 65

format(p1)
format p1

printf("this{p1}is{p2}a{p3}sample{p4}")
printf "this{0}is{1}a{2}sample{3}"
format p1
format p2
format p3
format p4


c.控制io

1.缓存刷新

dflush(dp1)
drawflush dp1

pflush(msg1)
printflush msg1

2.获取连接

result=getlink(p1)
getlink result p1

3.传感

result=block1.sensor(p1)
sensor result block1 p1

4.雷达探测

雷达探测采用链式输入参数
参数0-2为目标类型,范围:
{any enemy ally player attacker flying boss ground}
参数3为筛选器类型,范围:
{distance health shield armor maxHealth}
参数5为排序方式,1为正序,0为倒序;
若目标类型参数不足3个,则使用any填充,目标类型若无,默认为enemy,
筛选器若无,默认为distance,
排序方式若无,则默认为正序1;

result=radar(turret1).target(enemy,ground).order(0).sort(distance)
radar enemy ground any distance turret1 0 result

result=radar(turret1)
radar enemy any any distance turret1 1 result

5.控制输出
分为5种,范围:
{enabled shoot shootp config color}

enabled,config,color接受1参数
shoot,shootp接受1参数,同时链式连接to
to只会链式连接在shoot/shootp后,连接shoot时接受2参数,shootp时接受1参数

block1.enabled(p1)
control enabled block1 p1 0 0 0

block1.shoot(st).to(x,y)
control shoot block1 x y st 0

block1.shoot(st).to(unit1)
control shoot block1 unit1 st 0 0

6.顺序控制
按照示例转换

wait(p1)
wait p1

stop
stop

end
end

7.单位命令控制

绑定单位

ubind(p1)
ubind p1

单位雷达探测

单位雷达探测采用链式输入参数
参数0-2为目标类型,范围:
{any enemy ally player attacker flying boss ground}
参数3为筛选器类型,范围:
{distance health shield armor maxHealth}
参数4固定为0,表示当前所控制单位
参数5为排序方式,1为正序,0为倒序;
若目标类型参数不足3个,则使用any填充,目标类型若无,默认为enemy,
筛选器若无,默认为distance,
排序方式若无,则默认为正序1;

result=uradar().target(enemy,ground).order(0).sort(distance)
radar enemy ground any distance 0 0 result

result=uradar()
radar enemy any any distance 0 1 result

单位地形探测

单位地形探测采用链式输入参数
参数0为目标类型,范围:
{ore building spawn damaged}
参数1为建筑组,范围:
{core storage generator turret factory repair battery reactor drill shield},缺省值core
参数2为敌我判断,敌人1友军0,缺省值0
参数3为采矿种类,范围略,缺省值@copper
参数4-6跟随参数7命名,分别加后缀.x,.y,.f

bd=ulocate(ore).ore(@lead)
ulocate ore core 0 @lead bd.x bd.y bd.f bd

bd=ulocate(building).build(repair)
ulocate building repair 0 @copper bd.x bd.y bd.f bd

bd=ulocate(spawn)
ulocate spawn core 0 @copper bd.x bd.y bd.f bd

bd=ulocate(damaged)
ulocate damaged core 0 @copper bd.x bd.y bd.f bd

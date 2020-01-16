# 日志查看器
## intl-logview

### 消息交互数据结构
SocketMessage 
```
{
	String msg : 消息内容
	Integer type : 消息类型
	String error : 消息包含的错误
	String pattern : 消息携带的正则过滤条件
	FilterParams : params 消息携带的过滤条件
}
```



### 服务端发给客户端的消息
- type=0 表示 此消息为暂停操作
	msg=“0” 为 服务端为非暂停的状态
	msg=“1” 为 服务端为暂停的状态
	其他值均为null

- type =1 表示 此消息为未检查的日志消息
	msg 表示 日志内容
	其他值均为null

- type=2 表示 此消息为错误的日志消息
	msg 表示 日志内容
	error 表示日志错误信息
其他值均为null

- type=3 表示 此消息为正确的日志消息
	msg 表示 日志内容
其他值均为null

- type=4 表示 此消息为筛选按钮可用操作
其他值均为null

- type=5表示 此消息为清屏操作
其他值均为null

- type=6 表示 此消息为心跳操作
	msg =”heart”
其他值均为null


### 客户端发给服务端的消息
- msg = “pause”  暂停操作
- msg = ”watch”  开始监听操作
- msg = “rewatch”  过滤操作
- msg=”heart” 心跳操作
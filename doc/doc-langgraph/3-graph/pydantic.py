from pydantic import BaseModel
class MyState(BaseModel):
    a: int
    b: str="default"
# 自动校验
state = MyState(a=1)
print(state.a)
print(state.b)
# 类型错误会报错
state = MyState(a="aaa")
print(state.a)
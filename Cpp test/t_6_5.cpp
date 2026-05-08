#include <iostream>
using namespace std;

//定义抽象基类 Shape
class Shape
{
public:
    virtual double area() const = 0; //纯虚函数
};

//定义 Circle（圆形）类
class Circle:public Shape
{
public:
    Circle(double r):radius(r){} //构造函数
    virtual double area() const {return 3.14159*radius*radius;} //定义虚函数
protected:
    double radius; //半径
};

//定义 Square（正方形）类
class Square:public Shape
{
public:
    Square(double s):side(s){} //构造函数
    virtual double area() const {return side*side;} //定义虚函数
protected:
    double side;
};

//定义 Rectangle（矩形）类
class Rectangle:public Shape
{
public:
    Rectangle(double w,double h):width(w),height(h){} //构造函数
    virtual double area() const {return width*height;} //定义虚函数
protected:
    double width,height; //宽与高
};

//定义 Trapezoid（梯形）类
class Trapezoid:public Shape
{
public:
    Trapezoid(double t,double b,double h):top(t),bottom(b),height(h){} //构造函数
    virtual double area() const {return 0.5*(top+bottom)*height;} //定义虚函数
protected:
    double top,bottom,height; //上底、下底与高
};

//定义 Triangle（三角形）类
class Triangle:public Shape
{
public:
    Triangle(double w,double h):width(w),height(h){} //构造函数
    virtual double area() const {return 0.5*width*height;} //定义虚函数
protected:
    double width,height; //宽与高
};

int main()
{
    Circle circle(12.6); //建立 Circle 类对象 circle
    Square square(3.5); //建立 Square 类对象 square
    Rectangle rectangle(4.5,8.4); //建立 Rectangle 类对象 rectangle
    Trapezoid trapezoid(2.0,4.5,3.2); //建立 Trapezoid 类对象 trapezoid
    Triangle triangle(4.5,8.4); //建立 Triangle 类对象

    Shape *pt[5]={&circle,&square,&rectangle,&trapezoid,&triangle};//定义基类指针数组 pt，使它每一个元素指向一个派生类对象

    double areas=0.0; //areas 为总面积
    for(int i=0;i<5;i++)
    {
        areas=areas+pt[i]->area(); //累加面积
    }
    cout<<"total of all areas="<<areas<<endl; //输出总面积

    return 0;
}

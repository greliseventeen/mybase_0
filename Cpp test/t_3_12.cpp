#include <iostream>
using namespace std;

class Time;

class Date{
    public:
        Date(int,int,int);
        friend Time;
    private:
        int year;
        int month;
        int day;

};

Date:: Date(int y,int m,int d):year(y),month(m),day(d){}


class Time{
    public:
        Time(int,int,int);
        void display(const Date &);
    private:
        int hour;
        int minute;
        int second;

};

Time:: Time(int h,int m,int s):hour(h),minute(m),second(s){}

void Time:: display(const Date &d){
    cout<<d.month<<"/"<<d.day<<"/"<<d.year<<endl;
    cout<<hour<<":"<<minute<<":"<<second<<endl;

}

int main(){
    Time t1(12,12,12);
    Date d1(12,12,2012);

    t1.display(d1);

    return 0;
}


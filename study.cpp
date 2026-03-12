#include<iostream>
using namespace std;

class Box{
    public:
      Box(int,int,int);
      void set_pra();
      void cal_vol();
      void show_vol();

    private:
      int l;
      int w;
      int h;
      int vol;

};

Box:: Box(int l,int w,int h){
    this->l=l;
    this->w=w;
    this->h=h;

}

void Box:: set_pra(){
    cout<<"please enter the prameter:"<<endl;
    cin>>this->l;
    cin>>this->w;
    cin>>this->h;
}

void Box:: cal_vol(){
    this->vol=this->l*this->w*this->h;

}

void Box:: show_vol(){
    cout<<this->vol<<endl;

}

int main(){
    Box b1(10,10,10);
    b1.set_pra();
    b1.cal_vol();
    b1.show_vol();

    Box b2(10,10,10);
    b2.set_pra();
    b2.cal_vol();
    b2.show_vol();

    Box b3(10,10,10);
    b3.set_pra();
    b3.cal_vol();
    b3.show_vol();
    return 0;
}

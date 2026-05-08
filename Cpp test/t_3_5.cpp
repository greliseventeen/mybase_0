#include <iostream>
using namespace std;

class Student{
    public:
    Student(int n,float s):num(n),score(s){};
    void set_pra();
    int num;
    float score;

};

void Student:: set_pra(){
    cin>>num;
    cin>>score;

}


int main(){
    Student Stu[5]={Student(01,78.5),Student(02,88.5),Student(03,78.5),Student(04,88.5),Student(05,98.5)};
    void max(Student*);
    Student *p=Stu;

    max(p);

    return 0;
}

void max(Student* arr){
    float max_s= (*arr).score;
    int k=0;
    for(int i=0;i<5;i++)
        if((*(arr+i)).score>max_s)
        {
            max_s= (*(arr+i)).score;
            k=i;
        }
    cout<<(*(arr+k)).num<<"   "<<max_s<<endl;

}

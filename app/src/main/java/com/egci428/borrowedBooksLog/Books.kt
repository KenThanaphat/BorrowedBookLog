package com.egci428.borrowedBooksLog

class Books (val bookId: String,val bookName: String, val timeBorrowed: String, val returnTime: String, val imagePath: String){
    constructor() : this("","", "", "" , "")
}
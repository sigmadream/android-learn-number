package com.sangkon.numbers

object MathCore {
    init {
        System.loadLibrary("numbers")
    }
    
    // Returns [op1, op2, operator(0:+, 1:-, 2:*, 3:/), answer, wrong1, wrong2, wrong3]
    external fun generateProblem(level: Int): IntArray
}

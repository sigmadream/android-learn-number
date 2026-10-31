#include <jni.h>
#include <vector>
#include <algorithm>
#include <random>

static std::mt19937& get_rng() {
    static std::random_device rd;
    static std::mt19937 gen(rd());
    return gen;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_sangkon_numbers_MathCore_generateProblem(JNIEnv *env, jobject /* this */, jint level) {
    auto& gen = get_rng();

    // level 1: Addition only (sum <= 10)
    // level 2: Addition & Subtraction (numbers 1~10, result positive)
    // level 3: Addition, Subtraction, Multiplication, Division (1~10)
    int op = 0;
    if (level <= 1) {
        op = 0; // + only
    } else if (level == 2) {
        std::uniform_int_distribution<int> opDist(0, 1);
        op = opDist(gen); // + or -
    } else {
        std::uniform_int_distribution<int> opDist(0, 3);
        op = opDist(gen); // +, -, *, /
    }

    int op1 = 1, op2 = 1, answer = 2;

    if (op == 0) { // Addition
        int maxVal = (level <= 1) ? 5 : 9;
        std::uniform_int_distribution<int> dist(1, maxVal);
        op1 = dist(gen);
        op2 = dist(gen);
        answer = op1 + op2;
    } else if (op == 1) { // Subtraction (positive results only)
        int maxVal = (level <= 2) ? 5 : 9;
        std::uniform_int_distribution<int> distAns(1, maxVal);
        std::uniform_int_distribution<int> distOp2(1, maxVal);
        op2 = distOp2(gen);
        answer = distAns(gen);
        op1 = answer + op2;
    } else if (op == 2) { // Multiplication (simple times table 1..5)
        std::uniform_int_distribution<int> dist(1, 5);
        op1 = dist(gen);
        op2 = dist(gen);
        answer = op1 * op2;
    } else { // Division (exact division only)
        std::uniform_int_distribution<int> dist(1, 5);
        op2 = dist(gen);
        answer = dist(gen);
        op1 = op2 * answer;
    }

    std::vector<int> wrongs;
    std::uniform_int_distribution<int> deltaDist(-3, 3);
    std::uniform_int_distribution<int> fallbackDist(1, 5);

    int attempts = 0;
    while (wrongs.size() < 3 && attempts < 50) {
        attempts++;
        int delta = deltaDist(gen);
        int w = answer + delta;
        if (w <= 0 || w == answer) {
            w = answer + fallbackDist(gen);
        }
        if (w != answer && std::find(wrongs.begin(), wrongs.end(), w) == wrongs.end()) {
            wrongs.push_back(w);
        }
    }
    // Fill remaining if needed
    int fallbackVal = answer + 1;
    while (wrongs.size() < 3) {
        if (fallbackVal != answer && std::find(wrongs.begin(), wrongs.end(), fallbackVal) == wrongs.end()) {
            wrongs.push_back(fallbackVal);
        }
        fallbackVal++;
    }

    jintArray result = env->NewIntArray(7);
    jint fill[7];
    fill[0] = op1;
    fill[1] = op2;
    fill[2] = op;
    fill[3] = answer;
    fill[4] = wrongs[0];
    fill[5] = wrongs[1];
    fill[6] = wrongs[2];

    env->SetIntArrayRegion(result, 0, 7, fill);
    return result;
}

package jp.techacademy.kentaro.kawamura.qa_app

import java.io.Serializable
//これはQuestionクラスのanswers配列の中にあるデータひとつひとつ

class Answer(val body: String, val name: String, val uid: String, val answerUid: String) : Serializable


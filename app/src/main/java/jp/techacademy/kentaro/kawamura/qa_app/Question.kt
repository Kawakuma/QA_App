package jp.techacademy.kentaro.kawamura.qa_app

import java.io.Serializable
import java.util.ArrayList

class Question(val title: String, val body: String, val name: String, val uid: String,
  val questionUid: String, val genre: Int, bytes: ByteArray, val answers: ArrayList<Answer>) : Serializable {

//QuestionSendActivityでは画像をバイト配列に変換してimageキーに保存した。
// そのimageデータをMainのmEventListenerにてbytesに渡した。このbytesをQuestionの引数bytes: ByteArrayに渡す。

    val imageBytes: ByteArray

    init {
        imageBytes = bytes.clone()
        //保存先の参照先を別のもので確保しなおしている。デコードしたときにbytesのほうにも影響が出るためクローンを使ってimageBytesに新しく確保しなおしている。

//imageBytes = bytesにした後、imageBytesになにか操作をすれば、bytesにも同じ影響が出る。
// なのでcloneで値をコピーして、それぞれの影響を受けないようにする必要がある。
    }

}
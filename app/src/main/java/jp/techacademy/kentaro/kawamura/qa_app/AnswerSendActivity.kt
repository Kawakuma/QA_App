package jp.techacademy.kentaro.kawamura.qa_app

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_answer_send.*

class AnswerSendActivity : AppCompatActivity(), View.OnClickListener, DatabaseReference.CompletionListener {
//DatabaseReferenceのCompletionListenerでonCompleteを使えるようにする
    private lateinit var mQuestion: Question

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_answer_send)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras//extrasでキーを付加して
        mQuestion = extras!!.get("question") as Question//そのキーでデータをgetする

        // UIの準備
        sendButton.setOnClickListener(this)
    }

    override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
        //firebaseへの処理(保存完了or保存失敗)が終わったら呼ばれる?
        progressBar.visibility = View.GONE //処理が終了したらバーを見えない状態にする

        if (databaseError == null) {
            finish()// エラーなしならそのまま終了
        } else {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.send_answer_failure), Snackbar.LENGTH_LONG).show()
        }

    }

    override fun onClick(v: View) {
        // キーボードが出てたら閉じる
        val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        val answerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        //保存先を参照できるようにする。後にanswerRef.push().setValue(data, this)で保存を行う。 //questionUidは質問のID。UIDは質問者のID。

        val data = HashMap<String, String>()

        // UID
        data["uid"] = FirebaseAuth.getInstance().currentUser!!.uid
        //質問者のユーザー情報を取得　保存

        // 表示名 // Preferenceから名前を取る
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val name = sp.getString(NameKEY, "")
        data["name"] = name!!

        // 回答を取得する
        val answer = answerEditText.text.toString()

        if (answer.isEmpty()) {
            // 回答が入力されていない時はエラーを表示するだけ
            Snackbar.make(v, getString(R.string.answer_error_message), Snackbar.LENGTH_LONG).show()
            return
        }
        data["body"] = answer

        progressBar.visibility = View.VISIBLE//バーを見える状態にしておく
        answerRef.push().setValue(data, this)
    }

}
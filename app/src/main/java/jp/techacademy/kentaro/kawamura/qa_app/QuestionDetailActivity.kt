package jp.techacademy.kentaro.kawamura.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_question_detail.*

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mFavorite: Favorite //◀

    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference


    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDataBaseReference: DatabaseReference
    var StarCheck = 0 //0の時登録されてない。１の時登録されている。


    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            val answerUid = dataSnapshot.key ?: ""//アンサーはないときもある。
            //追加されたデータ(dataSnapshot)にはFirebaseが自動でID(key)をつける。

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }
            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {
        }

        override fun onCancelled(databaseError: DatabaseError) {
        }
    }

    override fun onResume() {
        super.onResume()
        //ここでログイン状態を確認してお気に入りボタンを表示させたりする
        mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser
        mDataBaseReference = FirebaseDatabase.getInstance().reference
        if (user != null) {//ログインしているなら
            val FavoRef =
                mDataBaseReference.child(Favo).child(user!!.uid).child(mQuestion.questionUid)
            FavoRef.addChildEventListener(FavoEventListener)//リスナー登録。onCreat内でも良い？

            FavoButton.visibility = View.VISIBLE//ボタン表示する
        } else {
            FavoButton.visibility = View.INVISIBLE
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        if (StarCheck == 1) {
            FavoButton.setImageResource(R.drawable.ic__star)
        } else {
            FavoButton.setImageResource(R.drawable.ic__star_border)
        }

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras //intentに追加情報を与える
        mQuestion =
            extras!!.get("question") as Question//追加情報のキーはquestionとする。MainからこのキーでQuestionデータを送り込むため。

        val extras2 = intent.extras
        var mGenre = extras2!!.get("genre")//◀Mainから送ったｼﾞｬﾝﾙを保持

        title = mQuestion.title
        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()


        //▼▼▼
        mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser
        if (user != null) {
            mDataBaseReference = FirebaseDatabase.getInstance().reference
            val FavoRef =
                mDataBaseReference.child(Favo).child(user!!.uid).child(mQuestion.questionUid)




            FavoButton.setOnClickListener {
                if (StarCheck == 0) {
                    FavoButton.setImageResource(R.drawable.ic__star)
                    StarCheck = 1
                    var data = HashMap<String, String>()

                    data["Genre"] = mGenre.toString()
                    FavoRef.setValue(data)


                } else {
                    FavoButton.setImageResource(R.drawable.ic__star_border)
                    StarCheck = 0
                    FavoRef.removeValue()
                }
            }
        } else {
        }
        //▲▲▲


        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)//Mainから受け取ったmQuestionを、さらにAnswerSendActivityに送る
            }
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString())
            .child(mQuestion.questionUid).child(AnswersPATH)//◆
        //AnswersPATH（つまりAnswers。ちなみにConst.ktで定義済み）を参照できるようにする。
        mAnswerRef.addChildEventListener(mEventListener)//リスナー設定

    }

    private val FavoEventListener = object : ChildEventListener {
        //このリスナーが呼ばれるということは、データの追加があったということ。つまりこのリスナーが呼ばれた時点で
        //お気に入り登録されているため、追加(お気に入り)されたときの処理はここに書く。
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            FavoButton.setImageResource(R.drawable.ic__star)
            StarCheck = 1
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {
        }

        override fun onCancelled(databaseError: DatabaseError) {
        }
    }


}




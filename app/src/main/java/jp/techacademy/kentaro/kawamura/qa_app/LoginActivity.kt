package jp.techacademy.kentaro.kawamura.qa_app


import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mCreateAccountListener: OnCompleteListener<AuthResult>
    private lateinit var mLoginListener: OnCompleteListener<AuthResult>
    private lateinit var mDataBaseReference: DatabaseReference

    // アカウント作成時にフラグを立て、ログイン処理後に名前をFirebaseに保存する
    private var mIsCreateAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mDataBaseReference = FirebaseDatabase.getInstance().reference
        //Firebaseのデータベースを参照できるようにインスタンス化

        // FirebaseAuthのオブジェクトを取得する
        mAuth = FirebaseAuth.getInstance() //ユーザー情報が入っている変数

        // ﾒｰﾙｱﾄﾞﾚｽ test2@test.jp,ﾊﾟｽﾜｰﾄﾞ 123456,表示名 kawamura2で登録してみた。

        mCreateAccountListener = OnCompleteListener { task ->
            if (task.isSuccessful) {
                // 成功した場合
                // ログインを行う
                val email = emailText.text.toString()
                val password = passwordText.text.toString()
                login(email, password)
            } else {

                // 失敗した場合
                // エラーを表示する
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, getString(R.string.create_account_failure_message), Snackbar.LENGTH_LONG).show()

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE
            }
        }

        // ログイン処理のリスナー
        mLoginListener = OnCompleteListener { task ->
            if (task.isSuccessful) {
                // ログインに成功した場合
                val user = mAuth.currentUser//今ログインしているユーザープロフィールをuserに渡す
                val userRef = mDataBaseReference.child(UsersPATH).child(user!!.uid)//userのuidを参照できるようにする

                //◆今ログインしているユーザーのﾌﾟﾛﾌｨｰﾙ(ｱﾄﾞﾚｽやﾊﾟｽﾜｰﾄﾞ)を164や156で取得して、そのﾌﾟﾛﾌｨｰﾙを使ってuidを指定している。
                // ｱﾄﾞﾚｽはどこに保存されているのか⇒firebaseのAuthenticationに保存されている
                //◆ﾘｱﾙﾀｲﾑﾃﾞｰﾀﾍﾞｰｽのYRFcakzh6cOStfIiNfoyclvaJ6z1がuid

                if (mIsCreateAccount) {
                    // アカウント作成の時は表示名をFirebaseに保存する
                    val name = nameText.text.toString()

                    val data = HashMap<String, String>() //dataをHashMapで保存できるようにする
                    data["name"] = name//dataに"name"というキーでname(nameTextからとったname)を保存する
                    userRef.setValue(data) //ここでfireBaseに保存している

                    // 表示名をPreferenceにも保存する
                    saveName(name)//名前をとってくる作業を短くするためpreferenceにも保存しているのかもしれない
                } else {//ログインする場合
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        // userRefにあるデータを一回だけ取ってくる
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val data = snapshot.value as Map<*, *>?  //Firebaseからデータ(value)をMap型に対応させて取り出す。
                            saveName(data!!["name"] as String)//◆ログインする時は、毎回nameを取り出してﾌﾟﾘﾌｧﾚﾝｽに保存(save)してる・・・これもわずかな効率化？
                        }//preferenceはスマホの保存空間？機種変更するとpreferenceに名前が入っていないので、ログイン時名前をpreference(そのスマホ)に保存する

                        override fun onCancelled(firebaseError: DatabaseError) {}
                    })
                }

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE

                // Activityを閉じる
                finish()

            } else {
                // 失敗した場合
                // エラーを表示する
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, getString(R.string.login_failure_message), Snackbar.LENGTH_LONG).show()

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE
            }
        }

        // タイトルの設定
        title = getString(R.string.login_title)

        createButton.setOnClickListener { v ->
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = passwordText.text.toString()
            val name = nameText.text.toString()

            if (email.length != 0 && password.length >= 6 && name.length != 0) {
                // ログイン時に表示名を保存するようにフラグを立てる
                mIsCreateAccount = true

                createAccount(email, password)
            } else {
                // エラーを表示する
                Snackbar.make(v, getString(R.string.login_error_message), Snackbar.LENGTH_LONG).show()
            }
        }

        loginButton.setOnClickListener { v ->
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = passwordText.text.toString()

            if (email.length != 0 && password.length >= 6) {
                // フラグを落としておく
                mIsCreateAccount = false

                login(email, password)
            } else {
                // エラーを表示する
                Snackbar.make(v, getString(R.string.login_error_message), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun createAccount(email: String, password: String) {
        // プログレスバーを表示する
        progressBar.visibility = View.VISIBLE

        // アカウントを作成する
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(mCreateAccountListener)
    }

    private fun login(email: String, password: String) {
        // プログレスバーを表示する
        progressBar.visibility = View.VISIBLE

        // ログインする
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(mLoginListener)
    }

    private fun saveName(name: String) {
        // Preferenceに保存する
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sp.edit()
        editor.putString(NameKEY, name)
        editor.commit()
    }
}
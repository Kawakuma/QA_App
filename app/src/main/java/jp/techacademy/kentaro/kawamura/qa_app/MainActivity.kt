package jp.techacademy.kentaro.kawamura.qa_app


import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.app_bar_main.*

import com.google.android.material.snackbar.Snackbar
import androidx.drawerlayout.widget.DrawerLayout

import com.google.firebase.database.*
import kotlinx.android.synthetic.main.content_main.*
import android.util.Base64
import android.view.View
import kotlinx.android.synthetic.main.activity_question_detail.*
import kotlinx.android.synthetic.main.app_bar_main.fab
import kotlinx.android.synthetic.main.content_main.listView


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    //private lateinit var mToolbar: Toolbar
    private var mGenre = 0

    var mfavo = 0//◀

    private lateinit var mDatabaseReference: DatabaseReference//firebaseを参照するための変数を初期化
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter
    private var mGenreRef: DatabaseReference? = null //◆ジャンルを参照するための変数を初期化。
    private var FavoRef: DatabaseReference? = null
    private lateinit var mAuth: FirebaseAuth

    private lateinit var mFavoriteArrayList: ArrayList<Favorite>



    private val mEventListener = object : ChildEventListener {
        //FireBaseのデータに追加・変化があった時に受け取るﾘｽﾅｰ。onNavigationItemSelectedで設定している。
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            // dataSnapshot : 追加された要素のKey-Value
            // s:追加された要素の一つ前の要素のkey名

            // onChildAddedはﾃﾞｰﾀの追加があったとき呼ばれるメソッド。第1引数はそのデータ(要素)そのもの。第2引数はその要素が何番目なのかを知るために利用できるｷｰ。
            val map = dataSnapshot.value as Map<String, String>//dataSnapshotのﾃﾞｰﾀ(value)をMapとしてmapに渡す
            val title = map["title"] ?: ""//◆キーがtitleのデータを変数titleに渡す
            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""//　この質問をしたユーザー情報
            val imageString = map["image"] ?: ""
            val bytes =
                if (imageString.isNotEmpty()) {
                    Base64.decode(imageString, Base64.DEFAULT)//元の画像に戻して、バイト配列に戻している
                } else {
                    byteArrayOf()// byteArrayOfのインスタンスを入れている　　nullの代わりに
                }

            val answerArrayList = ArrayList<Answer>()
            val answerMap = map["answers"] as Map<String, String>?//Mapの第1引数がキーとなっている
            if (answerMap != null) {
                for (key in answerMap.keys) {//◆keysで何をとってる？⇒回答のid. mapの中の質問リスト。
                    val temp = answerMap[key] as Map<String, String>//key番目の内容を保存していく。
                    val answerBody = temp["body"] ?: ""
                    val answerName = temp["name"] ?: ""
                    val answerUid = temp["uid"] ?: ""//何番目の回答か？
                    val answer = Answer(answerBody, answerName, answerUid, key)
                    //◆Answerクラスのオブジェクトを生成するために引数を与える。
                    answerArrayList.add(answer)//answerArrayListに↑のデータを渡し、↓のｺｰﾄﾞでQuestionｵﾌﾞｼﾞｪｸﾄに保存
                }
            }

            val question = Question(title, body, name, uid, dataSnapshot.key?: "",
                mGenre, bytes, answerArrayList)//リスナーが設定されると、データがすべてonAddを通る。
            // 入っているデータが１度すべてここを通る。だから全データが表示される。

            mQuestionArrayList.add(question)//mQuestionArrayListにquestionをセットして、後でdetailに飛ばす
            mAdapter.notifyDataSetChanged()//アダプターを更新。

            // 順番としてはonNavigationItemSelectedでItemを選択し、そのジャンルのﾃﾞｰﾀﾍﾞｰｽにリスナーを設定。ここでアダプターもセットしてる。mQuestionArrayListは空のまま？⇒
            // リスナー設定後、このonChildAddedなどが呼ばれ、データを取得する。(追加時も呼ばれる。)取得したデータをmQuestionArrayList.add(question)で渡して
            //アダプター更新。つまりデータを後入れして更新してる。
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            // onChildChangedはﾃﾞｰﾀに変化があったとき呼ばれるメソッド。
            //変更があったデータをdataSnapshot: DataSnapshotとしてとる。
            val map = dataSnapshot.value as Map<String, String>

            // 変更があったQuestionを探す
            for (question in mQuestionArrayList) {//配列をひとつづつチェックしていく
                if (dataSnapshot.key.equals(question.questionUid)) {//◆dataSnapshot.keyは何番目に保存されているかを示す？
                    question.answers.clear()//1度全消しして入れなおす
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            question.answers.add(answer)
                        }
                    }
                    mAdapter.notifyDataSetChanged()
                }
            }
        }
        override fun onChildRemoved(p0: DataSnapshot) {}

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

        override fun onCancelled(p0: DatabaseError) {}
    }





















    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)  //id名でActionBarのサポートを依頼




        // fabにClickリスナーを登録
        fab.setOnClickListener { view ->
            // ジャンルを選択していない場合（mGenre == 0）はエラーを表示するだけ
            // ◆まずif (mGenre == 0)で判断してからif (user == null) を実行？
            if (mGenre == 0) {
                Snackbar.make(
                    view,
                    getString(R.string.question_no_select_genre),
                    Snackbar.LENGTH_LONG
                ).show()
            } else {

            }
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // ジャンルを渡して質問作成画面を起動する
                val intent = Intent(applicationContext, QuestionSendActivity::class.java)
                intent.putExtra("genre", mGenre)

                startActivity(intent)
            }
        }

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle =
            ActionBarDrawerToggle(this, drawer, toolbar, R.string.app_name, R.string.app_name)
        //ActivityMainのドロワーをツールバーにセット//第４第５引数のR.の中身はint型。ドロワーを開いたとき閉めたときに、バーに説明文を表示する
        drawer_layout.addDrawerListener(toggle) //トグルをセットして
        toggle.syncState()//表示させる

        val navigationView = findViewById<NavigationView>(R.id.nav_view)//nav_viewをとってnavigationViewに渡す

        navigationView.setNavigationItemSelectedListener(this)
        //onClickListenerのitem版。nav_viewにあるItemのタップを受け付けるようにしている。nav_view.はactivity_mainにある。



        mDatabaseReference = FirebaseDatabase.getInstance().reference
        //これによりmDatabaseReferenceで参照できるようになる。onNavigationItemSelectedで使っている。

        mAdapter = QuestionsListAdapter(this)//アダプターのセット

        mQuestionArrayList = ArrayList<Question>()//Question型のArrayListが入る変数を定義
        mFavoriteArrayList = ArrayList<Favorite>()//◀
        mAdapter.notifyDataSetChanged()//アダプター更新


        listView.setOnItemClickListener{parent, view, position, id ->//これらの引数はタップした時に入るはず
            // 質問一覧にて質問をタップした時、詳細画面へ飛ぶように設定
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])//position番目のデータを送る

            intent.putExtra("genre",mGenre)//ジャンルも一緒に送る

            if (FavoRef != null) {//nullチェックせずにリムーブしようとしたためにヌルポとなった。nullのデータはリムーブできない！
                FavoRef!!.removeEventListener(FavoEventListener)
            }
            startActivity(intent)

        }

    }




    override fun onResume() {
        super.onResume()

        mQuestionArrayList.clear()

        //▼　ログインorログアウト処理から戻ってきた時、menuを非表示にするか否かの処理を行う。
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        var menu =navigationView.menu
        val user = FirebaseAuth.getInstance().currentUser
        var FavoMenu = menu.findItem(R.id.nav_favorite)
        if (user==null){FavoMenu.setVisible(false)}
        else{FavoMenu.setVisible(true)}
        //▲


        // 1:趣味を既定の選択とする
        if (mGenre == 0) {
            onNavigationItemSelected(nav_view.menu.getItem(0))//◆indexが０ということは1
        }//ActivityMainのnav_viewにあるmenu、ここに設定されているItemを渡してonNavigationItemSelectedを実行

    }




    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //ここでのitemはonCreateOptionsMenuのmenuInflater.inflate(R.menu.menu_main, menu)で紐づけられている
        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(applicationContext, SettingActivity::class.java)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }




    override fun onNavigationItemSelected(item: MenuItem): Boolean {    //ここで取ってくるitemはドロワーのアイテム。
        val id = item.itemId   //ここのアイテムはactivity_mainのapp:menu="@menu/activity_main_drawer"で紐づけられている。

        if (id == R.id.nav_hobby) {
            toolbar.title = getString(R.string.menu_hobby_label)
            mGenre = 1
        } else if (id == R.id.nav_life) {
            toolbar.title = getString(R.string.menu_life_label)
            mGenre = 2
        } else if (id == R.id.nav_health) {
            toolbar.title = getString(R.string.menu_health_label)
            mGenre = 3
        } else if (id == R.id.nav_compter) {
            toolbar.title = getString(R.string.menu_compter_label)
            mGenre = 4
        }
        else if (id == R.id.nav_favorite) {
            toolbar.title = getString(R.string.menu_favorite_label)
            mGenre = 5
            mfavo = 1//◀
        }
        if (mfavo==1){fab.visibility = View.INVISIBLE }

        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        //リスナー内のメソッドで得たデータをアダプターに渡す。setQuestionArrayListはQuenstionListAdapterで定義したメソッド。
        listView.adapter = mAdapter//アダプターをリストにセット。このListはcontent_mainで定義したリスト。
        // セットするためにimport kotlinx.android.synthetic.main.content_main.*で参照できるようにした。

        // 選択したジャンルにリスナーを登録する
        if (mGenreRef != null) {
            mGenreRef!!.removeEventListener(mEventListener)
            //設定されている旧リスナーを一度削除してから、選択したジャンルのリスナーを登録しなおす
             }
        mGenreRef = mDatabaseReference.child(ContentsPATH).child(mGenre.toString())//ｼﾞｬﾝﾙを参照できるようにする


        val user = FirebaseAuth.getInstance().currentUser//▼
        if (user!=null) {
            FavoRef = mDatabaseReference.child(Favo).child(user!!.uid)
        }

        //FaviRefがnullなのにリムーブしようとしていた　FavoRef!!.removeEventListener(FavoEventListener)
         if(mGenre==5&&user!=null){
             FavoRef!!.addChildEventListener(FavoEventListener)
            }else {
             mGenreRef!!.addChildEventListener(mEventListener)

         } //▲
        //ｼﾞｬﾝﾙに変化があった場合、リスナーが呼ばれるように設定する。
        //mEventListenerを設定したタイミングで、データがあるならばonChildAddedが呼ばれて、データの読み込み処理をする。
        drawer_layout.closeDrawer(GravityCompat.START)//◆ドロワーを戻すとき左側(START側)へ？
        return true
    }


    private val FavoEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            var FavoQid = dataSnapshot.key?: "" //keyがQIDとなる
            val FavoMap = dataSnapshot.value as Map<String, String>
            var genre = FavoMap["Genre"] ?: ""
                   val mContentGenreRef = mDatabaseReference.child(ContentsPATH).child(genre).child(FavoQid)

            mContentGenreRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = snapshot.value as Map<String,String>
                        val title = data["title"] ?: ""
                        val body = data["body"] ?: ""
                        val name = data["name"] ?: ""
                        val uid = data["uid"] ?: ""
                        val imageString = data["image"] ?: ""
                        val bytes =
                            if (imageString.isNotEmpty()) {
                                Base64.decode(imageString, Base64.DEFAULT)
                            } else {
                                byteArrayOf()
                            }
                        val answerArrayList = ArrayList<Answer>()
                        val answerMap = data["answers"] as Map<String, String>?
                        if (answerMap != null) {
                            for (key in answerMap.keys) {
                                val temp = answerMap[key] as Map<String, String>
                                val answerBody = temp["body"] ?: ""
                                val answerName = temp["name"] ?: ""
                                val answerUid = temp["uid"] ?: ""
                                val answer = Answer(answerBody, answerName, answerUid, key)
                                answerArrayList.add(answer)
                            }
                        }
                        mGenre = genre.toInt()
val Favoquestion = Question(title, body, name, uid, dataSnapshot.key?: "", mGenre, bytes, answerArrayList)
                        mQuestionArrayList.add(Favoquestion)
                        mAdapter.notifyDataSetChanged()


                }

                override fun onCancelled(firebaseError: DatabaseError) {}
            })
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
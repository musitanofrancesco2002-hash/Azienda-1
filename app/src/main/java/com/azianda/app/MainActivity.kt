package com.azianda.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val IT = Locale.ITALIAN
private val workerColors = listOf(
    Color(0xFF1677C8), Color(0xFF1EAD72), Color(0xFFF0A51A),
    Color(0xFF8A63D2), Color(0xFFE05A70), Color(0xFF4F718F),
    Color(0xFF00A7A0), Color(0xFFCE6B1F)
)

data class Worker(val id: String, val name: String, val note: String = "")

class Store(context: Context) {
    private val p = context.getSharedPreferences("azianda_data", Context.MODE_PRIVATE)

    fun workers(): List<Worker> {
        val s = p.getString("workers", "") ?: ""
        if (s.isBlank()) return emptyList()
        return s.split("||").mapNotNull {
            val a = it.split("::", limit = 3)
            if (a.size >= 2) Worker(a[0], a[1], a.getOrElse(2) { "" }) else null
        }
    }
    fun saveWorkers(list: List<Worker>) {
        p.edit().putString("workers", list.joinToString("||") {
            "${it.id}::${it.name.replace("::"," ")}::${it.note.replace("::"," ")}"
        }).apply()
    }
    fun hour(y:Int,m:Int,d:Int,id:String) = p.getString("h_${y}_${m}_${d}_$id","") ?: ""
    fun setHour(y:Int,m:Int,d:Int,id:String,v:String) = p.edit().putString("h_${y}_${m}_${d}_$id",v).apply()
}

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent { Azienda(Store(this)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Azienda(store: Store) {
    var workers by remember { mutableStateOf(store.workers()) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var showWorkers by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf<Worker?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var dataVersion by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Azienda", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton({ month = month.minusMonths(1) }) { Icon(Icons.Default.ChevronLeft, null) }
                },
                actions = {
                    IconButton({ month = month.plusMonths(1) }) { Icon(Icons.Default.ChevronRight, null) }
                    IconButton({ showWorkers = true }) { Icon(Icons.Default.People, "Operai") }
                }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(Color(0xFFF6F8FC))) {
            MonthBar(month, { month = it }, { scale = 1f })
            // Forza il ricalcolo dei totali quando cambia una cella.
            @Suppress("UNUSED_VARIABLE")
            val currentDataVersion = dataVersion
            if (workers.isEmpty()) {
                EmptyState { showWorkers = true }
            } else {
                MonthlyTable(
                    store = store,
                    month = month,
                    workers = workers,
                    scale = scale,
                    onScale = { scale = min(1.55f, max(0.75f, it)) },
                    onNotes = { showNotes = it },
                    onHourChanged = { dataVersion++ }
                )
            }
        }
    }

    if (showWorkers) {
        WorkerDialog(workers, { showWorkers = false }) {
            workers = it
            store.saveWorkers(it)
            showWorkers = false
        }
    }
    showNotes?.let { w ->
        NoteDialog(w, { showNotes = null }) { note ->
            workers = workers.map { if (it.id == w.id) it.copy(note = note) else it }
            store.saveWorkers(workers)
            showNotes = null
        }
    }
}

@Composable
fun MonthBar(month: YearMonth, onMonth:(YearMonth)->Unit, reset:()->Unit) {
    Card(Modifier.fillMaxWidth().padding(10.dp), colors=CardDefaults.cardColors(Color.White)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, null, tint=Color(0xFF1677C8))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(month.month.getDisplayName(TextStyle.FULL, IT).replaceFirstChar { it.uppercase() },
                    fontWeight=FontWeight.Bold, fontSize=18.sp)
                Text(month.year.toString(), fontSize=12.sp, color=Color.Gray)
            }
            TextButton({ onMonth(YearMonth.now()); reset() }) { Text("Oggi") }
        }
    }
}

@Composable
fun EmptyState(add:()->Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment=Alignment.Center) {
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Text("Ciao 🙂", fontSize=30.sp, fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Aggiungi i tuoi operai per iniziare.")
            Spacer(Modifier.height(16.dp))
            Button(add) { Icon(Icons.Default.Add,null); Spacer(Modifier.width(6.dp)); Text("Aggiungi operaio") }
        }
    }
}

@Composable
fun MonthlyTable(
    store:Store, month:YearMonth, workers:List<Worker>, scale:Float,
    onScale:(Float)->Unit, onNotes:(Worker)->Unit, onHourChanged:()->Unit
) {
    val hScroll = rememberScrollState()
    val listState = rememberLazyListState()
    val dateW = 86.dp * scale
    val workerW = 104.dp * scale
    val baseHeader = 56.dp * scale

    val totals = workers.associate { w ->
        w.id to (1..month.lengthOfMonth()).sumOf { d ->
            store.hour(month.year,month.monthValue,d,w.id).replace(",",".").toDoubleOrNull() ?: 0.0
        }
    }
    val grand = totals.values.sum()

    Column(Modifier.fillMaxSize()) {
        // Intestazione fissa: resta visibile mentre scorri le giornate.
        Row(
            Modifier
                .fillMaxWidth()
                .height(baseHeader)
                .background(Color(0xFF0B5B96))
                .pointerInput(Unit) {
                    detectTransformGestures { _,_,zoom,_ -> onScale(scale * zoom) }
                }
        ) {
            FixedCell("GIORNO\nDATA", dateW, Color(0xFF8EA2B8), Color.White, true)
            Row(Modifier.horizontalScroll(hScroll)) {
                workers.forEachIndexed { i,w ->
                    WorkerHeader(w.name, workerW, workerColors[i % workerColors.size])
                }
            }
        }

        // Le giornate scorrono verticalmente; la colonna Data resta ferma durante lo scroll orizzontale.
        LazyColumn(Modifier.weight(1f), state=listState) {
            items((1..month.lengthOfMonth()).toList()) { d ->
                val date = LocalDate.of(month.year,month.monthValue,d)
                val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp * scale)
                        .background(if (weekend) Color(0xFFFFE1E4) else Color.White)
                ) {
                    FixedDateCell(
                        "${date.dayOfWeek.getDisplayName(TextStyle.SHORT,IT).replaceFirstChar { it.uppercase() }}\n${"%02d".format(d)}",
                        dateW, weekend
                    )
                    Row(Modifier.horizontalScroll(hScroll)) {
                        workers.forEach { w ->
                            HourCell(store,month,d,w,workerW,scale,onHourChanged)
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(6.dp))
                // Seconda riga nomi: più piccola e ripetuta IMMEDIATAMENTE sopra i totali.
                Row(Modifier.fillMaxWidth().height(36.dp * scale).background(Color(0xFF34465C))) {
                    FixedCell("",dateW,Color(0xFF34465C),Color.White)
                    Row(Modifier.horizontalScroll(hScroll)) {
                        workers.forEachIndexed { i,w ->
                            Box(
                                Modifier.width(workerW).fillMaxHeight().border(0.5.dp,Color.White),
                                contentAlignment=Alignment.Center
                            ) {
                                Text(w.name, color=Color.White, fontSize=(11*scale).sp, fontWeight=FontWeight.Bold,
                                    textAlign=TextAlign.Center, maxLines=1)
                            }
                        }
                    }
                }
                // Totali.
                Row(Modifier.fillMaxWidth().height(50.dp * scale).background(Color(0xFF10233D))) {
                    FixedCell("TOTALE",dateW,Color(0xFF10233D),Color(0xFF66C7FF),true)
                    Row(Modifier.horizontalScroll(hScroll)) {
                        workers.forEach { w ->
                            Box(Modifier.width(workerW).fillMaxHeight().border(0.5.dp,Color(0xFF8090A4)),
                                contentAlignment=Alignment.Center) {
                                Text(formatHours(totals[w.id] ?: 0.0), color=Color.White,
                                    fontWeight=FontWeight.ExtraBold, fontSize=(17*scale).sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                // Blocco note separato: non attaccato al totale.
                Text("ANNOTAZIONI", Modifier.padding(horizontal=10.dp), fontWeight=FontWeight.ExtraBold, color=Color(0xFF29435F))
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    Box(Modifier.width(dateW).height(126.dp).background(Color(0xFFE7EDF5)).border(1.dp,Color(0xFFB8C5D4)),
                        contentAlignment=Alignment.Center) {
                        Text("NOTE",fontWeight=FontWeight.Bold)
                    }
                    workers.forEachIndexed { i,w ->
                        Card(Modifier.width(workerW).height(126.dp).padding(horizontal=3.dp),
                            colors=CardDefaults.cardColors(Color.White), border=BorderStroke(1.dp,Color(0xFFD1DAE4))) {
                            Column(Modifier.padding(7.dp)) {
                                Text(w.name, fontSize=(12*scale).sp, fontWeight=FontWeight.Bold,
                                    color=workerColors[i % workerColors.size], maxLines=1)
                                Spacer(Modifier.height(4.dp))
                                Text(w.note.ifBlank{"Scrivi un'annotazione…"}, fontSize=(11*scale).sp,
                                    color=if(w.note.isBlank()) Color.Gray else Color(0xFF25374A),
                                    maxLines=3, modifier=Modifier.weight(1f))
                                OutlinedButton({onNotes(w)}, modifier=Modifier.fillMaxWidth().height(30.dp)) {
                                    Icon(Icons.Default.Edit,null,Modifier.size(14.dp))
                                    Spacer(Modifier.width(3.dp)); Text("Modifica",fontSize=10.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.padding(horizontal=10.dp), verticalAlignment=Alignment.CenterVertically) {
                    Text("Totale azienda", fontWeight=FontWeight.Bold, modifier=Modifier.weight(1f))
                    Text("${formatHours(grand)} ore", fontSize=20.sp, fontWeight=FontWeight.ExtraBold, color=Color(0xFF1677C8))
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun FixedCell(text:String,w:Dp,bg:Color,fg:Color,bold:Boolean=false) {
    Box(Modifier.width(w).fillMaxHeight().background(bg).border(.5.dp,Color(0xFFB8C5D4)),contentAlignment=Alignment.Center) {
        Text(text,color=fg,fontSize=if(bold) 12.sp else 11.sp,fontWeight=if(bold) FontWeight.Bold else FontWeight.Normal,textAlign=TextAlign.Center)
    }
}
@Composable
fun FixedDateCell(text:String,w:Dp,weekend:Boolean) {
    Box(Modifier.width(w).fillMaxHeight().background(if(weekend)Color(0xFFFFC8CE) else Color(0xFFEAF0F7)).border(.5.dp,Color(0xFFB8C5D4)),contentAlignment=Alignment.CenterStart) {
        Text(text,Modifier.padding(start=8.dp),fontWeight=FontWeight.Bold,fontSize=11.sp,
            color=if(weekend)Color(0xFFB0212D) else Color(0xFF172A3D))
    }
}
@Composable
fun WorkerHeader(name:String,w:Dp,c:Color) {
    Box(Modifier.width(w).fillMaxHeight().background(c).border(.5.dp,Color.White),contentAlignment=Alignment.Center) {
        Text(name,fontSize=12.sp,fontWeight=FontWeight.ExtraBold,color=Color.White,textAlign=TextAlign.Center,maxLines=2)
    }
}
@Composable
fun HourCell(store:Store,month:YearMonth,day:Int,w:Worker,width:Dp,scale:Float,onHourChanged:()->Unit) {
    var v by remember(month,w.id,day) { mutableStateOf(store.hour(month.year,month.monthValue,day,w.id)) }
    Box(Modifier.width(width).fillMaxHeight().border(.5.dp,Color(0xFFD1DAE4)),contentAlignment=Alignment.Center) {
        OutlinedTextField(
            value=v,onValueChange={
                val clean=it.filter { ch -> ch.isDigit() || ch==',' || ch=='.' }.take(5)
                v=clean; store.setHour(month.year,month.monthValue,day,w.id,clean); onHourChanged()
            },singleLine=true,placeholder={Text("ore",fontSize=(10*scale).sp)},
            textStyle=LocalTextStyle.current.copy(textAlign=TextAlign.Center,fontSize=(13*scale).sp),
            modifier=Modifier.fillMaxWidth().padding(2.dp),shape=RoundedCornerShape(7.dp)
        )
    }
}

@Composable
fun WorkerDialog(old:List<Worker>,close:()->Unit,save:(List<Worker>)->Unit) {
    var list by remember { mutableStateOf(old) }
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=close,title={Text("Operai")},text={
        Column {
            list.forEach { w ->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(w.name,Modifier.weight(1f)); IconButton({list=list.filterNot{it.id==w.id}}){Icon(Icons.Default.Delete,null)}
                }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(name,{name=it},label={Text("Nome e cognome")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Button({
                if(name.isNotBlank()){list=list+Worker(System.currentTimeMillis().toString(),name.trim());name=""}
            },Modifier.fillMaxWidth()){Icon(Icons.Default.Add,null);Spacer(Modifier.width(5.dp));Text("Aggiungi")}
        }
    },confirmButton={Button({save(list)}){Text("Salva")}},dismissButton={TextButton(close){Text("Annulla")}})
}

@Composable
fun NoteDialog(w:Worker,close:()->Unit,save:(String)->Unit) {
    var note by remember { mutableStateOf(w.note) }
    AlertDialog(onDismissRequest=close,title={Text("Annotazioni — ${w.name}")},text={
        OutlinedTextField(note,{note=it},label={Text("Scrivi qui")},minLines=6,modifier=Modifier.fillMaxWidth())
    },confirmButton={Button({save(note)}){Text("Salva")}},dismissButton={TextButton(close){Text("Annulla")}})
}

fun formatHours(v:Double):String {
    val x=(v*100).roundToInt()/100.0
    return if(x%1.0==0.0)x.toInt().toString() else x.toString().replace(".",",")
}

package br.com.appforge.mobile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.util.UUID

class MainActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme=darkColorScheme(
            primary=Color(0xFF93ADFF), surface=Color(0xFF1A2335),
            background=Color(0xFF111827))) { TelaAppForge() } }
    }
}

private fun cor(nome:String)=when(nome) {
    "Verde"->Color(0xFF287D5F)
    "Roxo"->Color(0xFF805CD4)
    else->Color(0xFF4269CC)
}

@Composable
fun TelaAppForge() {
    val context=LocalContext.current
    val repositorio=remember { Armazem(context) }
    val prefs=remember { context.getSharedPreferences("sessao",0) }
    var projetos by remember { mutableStateOf(repositorio.load()) }
    var atualId by remember { mutableStateOf(prefs.getString("ultimo",null)) }
    var pagina by remember { mutableIntStateOf(0) }
    var escolhido by remember { mutableStateOf<String?>(null) }
    var backup by remember { mutableStateOf<Projeto?>(null) }
    var nomeDialog by remember { mutableStateOf(false) }
    var apagarDialog by remember { mutableStateOf(false) }

    fun salvar(p:Projeto) {
        val nova=projetos.map { if(it.id==p.id) p else it }
        repositorio.save(nova)
        projetos=nova
    }
    fun abrir(p:Projeto) {
        atualId=p.id; prefs.edit().putString("ultimo",p.id).apply()
        escolhido=null; pagina=0
    }
    val exportar=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if(uri!=null) {
            val p=backup
            if(p!=null) runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(ProjetosJson.encode(p).toString(2).toByteArray(Charsets.UTF_8))
                } ?: error("Arquivo indisponível")
            }.onSuccess { Toast.makeText(context,"Backup salvo",Toast.LENGTH_SHORT).show() }
              .onFailure { Toast.makeText(context,"Falha ao exportar",Toast.LENGTH_LONG).show() }
        }
        backup=null
    }
    val importar=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) runCatching {
            val txt=context.contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}
                ?: error("Arquivo inacessível")
            require(txt.length<=1_000_000)
            ProjetosJson.decode(JSONObject(txt)).copy(id=UUID.randomUUID().toString())
        }.onSuccess { p ->
            val nova=projetos + p
            repositorio.save(nova); projetos=nova; abrir(p)
        }.onFailure { Toast.makeText(context,"Backup inválido",Toast.LENGTH_LONG).show() }
    }
    val atual=projetos.firstOrNull { it.id==atualId }
    Scaffold(topBar={ Surface(color=MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            if(atual!=null) {
                TextButton(onClick={atualId=null;prefs.edit().remove("ultimo").apply()}) { Text("‹ Projetos") }
                Text(atual.nome,modifier=Modifier.weight(1f),fontWeight=FontWeight.Bold)
            } else {
                Text("◈ AppForge Mobile",modifier=Modifier.weight(1f),
                    style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                Text("GRÁTIS",color=MaterialTheme.colorScheme.primary)
            }
        }
    } }) { padding ->
      if(atual==null) {
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Crie aplicativos pelo celular",style=MaterialTheme.typography.headlineSmall)
            Text("Escolha um modelo para personalizar textos, botões e campos.")
            listOf("Anotações","Finanças","Estudos","Estoque","Em branco").forEach { modelo ->
                ElevatedButton(onClick={
                    val p=Modelos.novo(modelo)
                    val nova=projetos+p
                    repositorio.save(nova); projetos=nova; abrir(p)
                }, modifier=Modifier.fillMaxWidth()) { Text("+ Novo: "+modelo) }
            }
            OutlinedButton(onClick={importar.launch(arrayOf("application/json","text/plain","*/*"))},
                modifier=Modifier.fillMaxWidth()) { Text("Importar backup JSON") }
            HorizontalDivider()
            Text("Meus projetos ("+projetos.size+")",style=MaterialTheme.typography.titleMedium)
            projetos.forEach { p ->
                ElevatedCard(onClick={abrir(p)},modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(p.nome,fontWeight=FontWeight.Bold)
                        Text(p.modelo+" • "+p.elementos.size+" elementos")
                    }
                }
            }
        }
      } else {
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex=pagina) {
                listOf("Prévia","Editar","Projeto").forEachIndexed { i,n ->
                    Tab(selected=pagina==i,onClick={pagina=i},text={Text(n)})
                }
            }
            when(pagina) {
                0 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                     verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("Prévia da tela",style=MaterialTheme.typography.titleLarge)
                    Surface(color=Color(0xFFF7F8FC),shape=RoundedCornerShape(20.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            Text(atual.nome,color=cor(atual.cor),fontWeight=FontWeight.Bold,
                                style=MaterialTheme.typography.headlineSmall)
                            atual.elementos.forEach { e ->
                                var preenchido by remember(e.id) { mutableStateOf("") }
                                var ligado by remember(e.id) { mutableStateOf(false) }
                                when(e.tipo) {
                                    "Texto"->Text(e.texto,color=Color(0xFF171F2F))
                                    "Campo"->OutlinedTextField(value=preenchido,onValueChange={preenchido=it},
                                        label={Text(e.texto)},modifier=Modifier.fillMaxWidth())
                                    "Botão"->Button(onClick={Toast.makeText(context,
                                        "Prévia: configure a lógica do aplicativo exportado",Toast.LENGTH_SHORT).show()},
                                        modifier=Modifier.fillMaxWidth()){Text(e.texto)}
                                    "Interruptor"->Row(verticalAlignment=Alignment.CenterVertically) {
                                        Text(e.texto,modifier=Modifier.weight(1f),color=Color.Black)
                                        Switch(checked=ligado,onCheckedChange={ligado=it})
                                    }
                                }
                            }
                        }
                    }
                    Text("A prévia permite testar o desenho; ainda não gera APKs de outros projetos.")
                }
                1 -> Column(Modifier.fillMaxSize().padding(14.dp),
                     verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("Editar componentes",style=MaterialTheme.typography.titleLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        listOf("Texto","Campo","Botão","Interruptor").forEach { tipo ->
                            OutlinedButton(onClick={
                                if(atual.elementos.size<100) {
                                    val e=Elemento(tipo=tipo,texto="Novo "+tipo.lowercase())
                                    salvar(atual.copy(elementos=atual.elementos+e))
                                    escolhido=e.id
                                }
                            }) { Text("+ "+tipo) }
                        }
                    }
                    LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        items(atual.elementos,key={it.id}) { e ->
                            OutlinedCard(onClick={escolhido=e.id},modifier=Modifier.fillMaxWidth()) {
                                Text(e.tipo+": "+e.texto,modifier=Modifier.padding(14.dp))
                            }
                        }
                    }
                    val atualElemento=atual.elementos.firstOrNull{it.id==escolhido}
                    if(atualElemento!=null) {
                        OutlinedTextField(value=atualElemento.texto,onValueChange={novo ->
                            salvar(atual.copy(elementos=atual.elementos.map { e ->
                                if(e.id==atualElemento.id) e.copy(texto=novo.take(300)) else e
                            }))
                        },label={Text("Texto do componente")},modifier=Modifier.fillMaxWidth())
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            val i=atual.elementos.indexOfFirst { it.id==atualElemento.id }
                            OutlinedButton(enabled=i>0,onClick={
                                val items=atual.elementos.toMutableList()
                                val prev=items[i-1];items[i-1]=items[i];items[i]=prev
                                salvar(atual.copy(elementos=items))
                            }){Text("↑")}
                            OutlinedButton(enabled=i>=0&&i<atual.elementos.lastIndex,onClick={
                                val items=atual.elementos.toMutableList()
                                val next=items[i+1];items[i+1]=items[i];items[i]=next
                                salvar(atual.copy(elementos=items))
                            }){Text("↓")}
                            OutlinedButton(onClick={
                                salvar(atual.copy(elementos=atual.elementos.filterNot{it.id==atualElemento.id}))
                                escolhido=null
                            }){Text("Excluir")}
                        }
                    } else Text("Selecione um componente para editar.")
                }
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                       verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("Configurações",style=MaterialTheme.typography.headlineSmall)
                    OutlinedButton(onClick={nomeDialog=true},modifier=Modifier.fillMaxWidth()) {
                        Text("Renomear: "+atual.nome)
                    }
                    Text("Cor principal")
                    Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                        listOf("Azul","Verde","Roxo").forEach { nome ->
                            FilterChip(selected=atual.cor==nome,onClick={salvar(atual.copy(cor=nome))},
                                label={Text(nome)})
                        }
                    }
                    Button(onClick={
                        backup=atual
                        exportar.launch(atual.nome.replace(Regex("[^a-zA-Z0-9]"),"-")+".json")
                    },modifier=Modifier.fillMaxWidth()) {Text("Exportar backup do projeto")}
                    Text("Todas as alterações são gravadas imediatamente na memória interna do celular.")
                    Text("A geração do código-fonte de outros aplicativos será adicionada em outra versão.")
                    OutlinedButton(onClick={apagarDialog=true},modifier=Modifier.fillMaxWidth()) {
                        Text("Excluir projeto",color=MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
      }
    }
    if(nomeDialog && atual!=null) {
        var nome by remember(atual.id) { mutableStateOf(atual.nome) }
        AlertDialog(onDismissRequest={nomeDialog=false},title={Text("Renomear aplicativo")},
            text={OutlinedTextField(value=nome,onValueChange={nome=it.take(80)},singleLine=true)},
            confirmButton={TextButton(enabled=nome.isNotBlank(),onClick={
                salvar(atual.copy(nome=nome.trim()));nomeDialog=false
            }){Text("Salvar")}},
            dismissButton={TextButton(onClick={nomeDialog=false}){Text("Cancelar")}})
    }
    if(apagarDialog && atual!=null) {
        AlertDialog(onDismissRequest={apagarDialog=false},title={Text("Excluir projeto?")},
            text={Text("Exclusão definitiva. Exporte um backup antes de excluir.")},
            confirmButton={TextButton(onClick={
                val nova=projetos.filterNot { it.id==atual.id }
                repositorio.save(nova);projetos=nova;atualId=null
                prefs.edit().remove("ultimo").apply();apagarDialog=false
            }){Text("Excluir")}},
            dismissButton={TextButton(onClick={apagarDialog=false}){Text("Cancelar")}})
    }
}

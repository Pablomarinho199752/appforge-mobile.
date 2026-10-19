package br.com.appforge.mobile

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Elemento(val id: String = UUID.randomUUID().toString(), val tipo: String, val texto: String)
data class Projeto(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val modelo: String,
    val cor: String = "Azul",
    val elementos: List<Elemento>
)

object Modelos {
    fun novo(modelo: String): Projeto {
        val itens = when (modelo) {
            "Anotações" -> listOf(Elemento(tipo="Texto",texto="Minhas anotações"), Elemento(tipo="Campo",texto="Escreva aqui"), Elemento(tipo="Botão",texto="Salvar nota"))
            "Finanças" -> listOf(Elemento(tipo="Texto",texto="Controle financeiro"), Elemento(tipo="Campo",texto="Descrição"), Elemento(tipo="Campo",texto="Valor"), Elemento(tipo="Botão",texto="Registrar"))
            "Estudos" -> listOf(Elemento(tipo="Texto",texto="Meus estudos"), Elemento(tipo="Campo",texto="Resposta"), Elemento(tipo="Botão",texto="Salvar resposta"))
            "Estoque" -> listOf(Elemento(tipo="Texto",texto="Meu estoque"), Elemento(tipo="Campo",texto="Produto"), Elemento(tipo="Campo",texto="Quantidade"), Elemento(tipo="Botão",texto="Cadastrar"))
            else -> listOf(Elemento(tipo="Texto",texto="Meu aplicativo"))
        }
        return Projeto(nome="Meu " + modelo, modelo=modelo, elementos=itens)
    }
}

object ProjetosJson {
    fun encode(p: Projeto): JSONObject = JSONObject().apply {
        put("id",p.id); put("nome",p.nome); put("modelo",p.modelo); put("cor",p.cor)
        put("elementos",JSONArray().apply { p.elementos.forEach { e ->
            put(JSONObject().apply { put("id",e.id);put("tipo",e.tipo);put("texto",e.texto) })
        } })
    }
    fun decode(o: JSONObject): Projeto {
        val array=o.getJSONArray("elementos")
        require(array.length() <= 100) { "Muitos componentes" }
        return Projeto(id=UUID.fromString(o.getString("id")).toString(),
            nome=o.getString("nome").take(80), modelo=o.optString("modelo","Pessoal"),
            cor=o.optString("cor","Azul"),
            elementos=(0 until array.length()).map {
                val e=array.getJSONObject(it)
                Elemento(id=UUID.fromString(e.getString("id")).toString(),
                    tipo=e.getString("tipo"), texto=e.getString("texto").take(300))
            })
    }
}

class Armazem(context: Context) {
    private val file=AtomicFile(File(context.filesDir,"appforge-projects.json"))
    fun load(): List<Projeto> = runCatching {
        val s=file.openRead().bufferedReader().use{it.readText()}
        val arr=JSONArray(s)
        (0 until arr.length()).map { ProjetosJson.decode(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())
    @Synchronized fun save(list: List<Projeto>) {
        val stream=file.startWrite()
        try {
            val arr=JSONArray().apply { list.forEach{put(ProjetosJson.encode(it))} }
            stream.write(arr.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch(e: Exception) { file.failWrite(stream); throw e }
    }
}

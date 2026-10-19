# AppForge Mobile — Android 0.1.0

Aplicativo gratuito, nativo Android, com Kotlin e Jetpack Compose.

## Primeiro protótipo
- Modelos: Anotações, Finanças, Estudos, Estoque e Em branco.
- Editor de elementos: textos, campos, botões e interruptores.
- Prévia da tela, troca de cor, reordenação e renomeação.
- Projetos gravados automaticamente com Android AtomicFile.
- Reabertura do último projeto e backup/importação JSON.

**Limites:** os modelos são exemplos de interface, não sistemas de negócios completos. O gerador de código-fonte de aplicativos derivados e o arrastar-e-soltar ainda não fazem parte desta versão hospedada.

## Obter o APK
No GitHub, abra **Actions > Gerar APK AppForge Mobile**. Após uma execução bem-sucedida, baixe o artefato **AppForge-Mobile-APK-teste**. Dentro do ZIP do GitHub estará o APK debug instalável (Android 8+).

## Cuidados com atualizações
O APK desta fase é debug. Executores diferentes do GitHub Actions podem assinar APKs debug com chaves diferentes. Portanto, atualizações podem exigir outra assinatura e impedir instalação sobre a versão antiga. Faça backup antes de qualquer desinstalação e use uma chave de assinatura release persistente para versões futuras. Não perca seus projetos.

# AutoPlayGM2

Projeto de estudo pessoal (**não comercial**) pra entender como o AAAD (Android
Auto Apps Downloader) consegue fazer apps de terceiros, instalados fora da
Play Store, aparecerem no Android Auto sem root.

O código-fonte real do AAAD não é aberto (o repositório público deles só
contém a casca do app — licença, telas de pagamento, UI). Este projeto
reimplementa o mecanismo do zero, a partir da API pública do Android, só
para aprendizado e uso próprio.

## Como funciona o truque

Desde 2018 o Android Auto só lista apps de terceiros se o campo
`installerPackageName` registrado pelo sistema para aquele pacote for
`com.android.vending` (Play Store). Um APK instalado via sideload normal
fica com esse campo vazio ou diferente, e por isso não aparece.

A API pública `PackageManager.setInstallerPackageName()` permite que **quem
instalou um pacote** reescreva esse campo depois, para qualquer string —
sem root e sem permissão especial, desde que o chamador seja o "installer de
registro" daquele pacote. É exatamente isso que este projeto faz.

## Estrutura

- **installer-app**: o app final, o único que você manda pra alguém testar.
  Tem um APK "cobaia" (test-auto-app) embutido dentro dele. Ao abrir e
  apertar o botão, ele instala esse APK embutido via `PackageInstaller.Session`
  e em seguida chama `setInstallerPackageName(pacote, "com.android.vending")`.
- **test-auto-app**: o app "cobaia" mínimo, com um `MediaBrowserService` e o
  `automotive_app_desc.xml` que o Android Auto exige pra reconhecer um app de
  mídia. Não tem nenhuma função real, é só o suficiente pra existir como app
  de mídia válido aos olhos do Android Auto.

## Build: sem Android Studio, direto na nuvem (grátis)

Como compilar Android localmente pesa demais (SDK completo, vários GB), este
projeto builda pelo **GitHub Actions**. Você não precisa instalar nada no seu
PC além do Git.

### 1. Subir o projeto pro GitHub

1. Crie uma conta no GitHub (se ainda não tiver) e um repositório novo,
   pode ser privado — ex: `autoplaygm2`.
2. Na pasta `AutoPlayGM2` (esta), rode:
   ```
   git init
   git add .
   git commit -m "primeiro commit"
   git branch -M main
   git remote add origin https://github.com/SEU_USUARIO/autoplaygm2.git
   git push -u origin main
   ```
   (Se não tiver `git` instalado, dá pra fazer o mesmo pelo GitHub Desktop,
   arrastando a pasta.)

### 2. Deixar o Actions buildar

Assim que o push terminar, vá na aba **Actions** do repositório no GitHub.
Vai aparecer um workflow rodando ("Build AutoPlayGM2 APK") — demora uns 5-8
minutos na primeira vez (ele baixa o Android SDK do zero).

Quando terminar (bolinha verde ✅), entre na execução e desça até
**Artifacts**. Baixe o arquivo `AutoPlayGM2` — é um `.zip` que contém o
`installer-app-debug.apk`. **Esse é o único arquivo que seu amigo precisa.**

### 3. Hospedar e mandar pro seu amigo

Suba o `installer-app-debug.apk` na sua hospedagem e manda o link direto pro
seu amigo. Recomendo renomear pra algo tipo `AutoPlayGM2.apk` antes de subir,
só pra ficar mais claro pra ele.

## Passo a passo do teste (pro seu amigo, no carro)

1. Baixar o `AutoPlayGM2.apk` pelo link, e abrir o arquivo baixado.
2. O Android vai avisar que é de "fonte desconhecida" — precisa permitir a
   instalação (é padrão pra qualquer APK fora da Play Store, não tem como
   evitar essa parte).
3. Abrir o app **AutoPlayGM2** e apertar **"Instalar app de teste"**.
4. Vai aparecer o diálogo clássico do sistema "Deseja instalar este app?" —
   confirmar.
5. Depois de instalar, o próprio app já tenta o truque sozinho e mostra na
   tela se deu certo.
6. Pode apertar **"Verificar se funcionou"** a qualquer momento pra confirmar.
7. Conectar o celular no Android Auto do carro (cabo ou sem fio) e ver se
   **"AutoPlayGM2 Test"** aparece na lista de apps de mídia.

Não precisa mexer em nenhuma configuração de desenvolvedor nem ativar
"fontes desconhecidas" dentro do próprio Android Auto — é justamente essa
etapa que o truque contorna.

## Conferir por fora (opcional, via ADB)

```
adb shell pm list packages -i | grep testautoapp
```

Deve mostrar `installer=com.android.vending`.

## Avisos

- Isso é só pra estudo e uso pessoal. Não redistribua em escala nem tente
  contornar apps pagos.
- A Google pode fechar essa brecha (mudar a checagem de permissão do
  `setInstallerPackageName`) numa atualização futura do Android — é o mesmo
  risco que o AAAD real assume.
- `test-auto-app` não tem nenhuma UI real nem toca nada de verdade, é só o
  mínimo pra existir como app de mídia válido.

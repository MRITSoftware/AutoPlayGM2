# AutoPlayGM2

Projeto de estudo pessoal (**não comercial**) pra entender como o AAAD (Android
Auto Apps Downloader) consegue fazer apps de terceiros, instalados fora da
Play Store, aparecerem no Android Auto sem root.

O código-fonte real do AAAD não é aberto (o repositório público deles só
contém a casca do app — licença, telas de pagamento, UI). Este projeto
reimplementa o mecanismo do zero, a partir da API pública do Android, só
para aprendizado e uso próprio.

## Como funciona (e o que descobrimos no caminho)

**Tentativa original**: desde 2018 o Android Auto só lista apps de terceiros
se o campo `installerPackageName` registrado pelo sistema for
`com.android.vending` (Play Store), e a API pública
`PackageManager.setInstallerPackageName()` deixa quem instalou um pacote
reescrever esse campo depois. Essa é a teoria por trás do truque do AAAD.

**O que rolou na prática**: em qualquer aparelho com a Play Store de verdade
instalada, o Android bloqueia essa reescrita especificamente quando o valor
que você quer colocar (`com.android.vending`) já existe no aparelho — o
sistema exige que o chamador tenha o **mesmo certificado de assinatura** do
pacote que está sendo citado. Como só a Google assina `com.android.vending`,
qualquer app nosso recebe `SecurityException: Caller does not have same
cert as new installer package`. É uma trava de segurança deliberada da
Google, não um bug — fechando exatamente essa classe de spoof.

**O caminho que realmente funciona hoje**: o próprio app Android Auto tem um
"modo desenvolvedor" oculto (destravado tocando 10x na versão, em
Configurações > Sobre, dentro do app Android Auto) com um toggle **"Fontes
desconhecidas"**. Ativar esse toggle libera apps de terceiros compatíveis
sem precisar de nenhum spoof de installer. É documentado e usado até hoje
pela comunidade pra apps como Fermata Auto, AA Mirror etc.

## Estrutura

- **installer-app**: o app final, o único que você manda pra alguém testar.
  Tem um APK "cobaia" (test-auto-app) embutido dentro dele. Ao abrir e
  apertar o botão, ele instala esse APK embutido via `PackageInstaller.Session`
  e mostra o passo a passo pra ativar o "Fontes desconhecidas" no Android
  Auto (tem até um botão pra abrir o Android Auto direto).
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
5. Depois de instalar, o app mostra o passo a passo pra ativar o modo
   desenvolvedor do Android Auto:
   - Abrir o app Android Auto no celular.
   - Ir em Configurações > Sobre (ou rolar até o final das configurações).
   - Tocar 10x seguidas na versão do app até aparecer o aviso de "Modo
     desenvolvedor ativado".
   - Voltar, entrar em "Configurações de desenvolvedor" (ou ícone de
     engrenagem/três pontinhos, dependendo da versão) e ativar **"Fontes
     desconhecidas"**.
6. Pode apertar **"Abrir Android Auto"** no AutoPlayGM2 pra ir direto pro app.
7. Conectar o celular no Android Auto do carro (cabo ou sem fio) e ver se
   **"AutoPlayGM2 Test"** aparece na lista de apps de mídia.

## Conferir por fora (opcional, via ADB)

```
adb shell pm list packages | grep testautoapp
```

Deve mostrar que o pacote está instalado (`package:com.autoplaygm2.testautoapp`).

## Avisos

- Isso é só pra estudo e uso pessoal. Não redistribua em escala nem tente
  contornar apps pagos.
- Diferente do que a gente imaginava no início, o mecanismo que realmente
  funciona hoje **não é** spoof de installer — é o toggle "Fontes
  desconhecidas" do próprio Android Auto (seção "Como funciona" acima
  explica por quê).
- `test-auto-app` não tem nenhuma UI real nem toca nada de verdade, é só o
  mínimo pra existir como app de mídia válido.

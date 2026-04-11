# Deploy automatico para producao

Este projeto agora possui deploy automatico no GitHub Actions:

- Workflow: `.github/workflows/deploy-production.yml`
- Trigger: `push` na branch `master`
- Fluxo: build Maven -> upload JAR na VPS -> para processo atual -> sobe nova versao

## 1) Criar/usar branch master

Se seu repositorio ainda usa `main`, crie e publique `master`:

```bash
git checkout -b master
git push -u origin master
```

Depois, configure `master` como branch padrao no GitHub (Settings -> Branches).

## 2) Configurar secrets no GitHub

No repositorio: Settings -> Secrets and variables -> Actions -> New repository secret

Secrets obrigatorios:

- `VPS_HOST`: IP ou dominio da VPS
- `VPS_USER`: usuario SSH
- `VPS_SSH_KEY`: chave privada SSH (conteudo completo)
- `VPS_APP_DIR`: pasta da aplicacao na VPS (ex: `/opt/gestao`)

Secrets opcionais:

- `VPS_PORT`: porta SSH (padrao 22)
- `VPS_JAVA_CMD`: comando Java (padrao `java`)
- `VPS_JVM_OPTS`: opcoes JVM (ex: `-Xms256m -Xmx512m -Dspring.profiles.active=prod`)

## 3) Como fica o deploy

A cada merge na `master`:

1. O workflow compila com `./mvnw -B clean package`
2. Copia o JAR para `/tmp/gestao-deploy` na VPS
3. Executa `deploy/deploy-vps.sh` remotamente
4. O script:
   - move o JAR para `VPS_APP_DIR/gestao.jar`
   - encerra o processo anterior (PID e fallback por nome do jar)
   - inicia novo processo com `nohup`
   - grava PID em `app.pid` e logs em `app.log`

## 4) Validar primeiro deploy

Depois de um merge na `master`, confira na VPS:

```bash
cd /opt/gestao
cat app.pid
tail -n 100 app.log
ps -fp "$(cat app.pid)"
```

## 5) Rollback manual rapido (opcional)

Se quiser rollback mais seguro no futuro, podemos evoluir para estrategia com:

- versoes por timestamp em `releases/`
- symlink `current.jar`
- rollback com um comando

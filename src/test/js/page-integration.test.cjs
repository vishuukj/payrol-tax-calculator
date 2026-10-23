/* Exercises the page's actual event handlers against a real Java server and temporary DB. */
const fs=require('node:fs');
const os=require('node:os');
const path=require('node:path');
const vm=require('node:vm');
const net=require('node:net');
const {spawn,spawnSync}=require('node:child_process');
const assert=require('node:assert/strict');
const {createDom}=require('./dom-harness.cjs');
const wait=ms=>new Promise(resolve=>setTimeout(resolve,ms));
async function until(condition){for(let count=0;count<150;count++){if(condition())return;await wait(40);}throw Error('UI did not reach expected state');}
async function freePort(){const server=net.createServer();await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));const port=server.address().port;await new Promise(resolve=>server.close(resolve));return port;}
(async()=>{
    const directory=fs.mkdtempSync(path.join(os.tmpdir(),'payroll-ui-'));
    const database=path.join(directory,'test.db');
    const port=await freePort(),base='http://127.0.0.1:'+port;
    const properties=spawnSync('java',['-XshowSettings:properties','-version'],{encoding:'utf8',windowsHide:true});
    const javaHome=(properties.stderr+'\n'+properties.stdout).match(/java.home\s*=\s*(.+)/)?.[1].trim();
    const java=javaHome?path.join(javaHome,'bin',process.platform==='win32'?'java.exe':'java'):'java';
    const child=spawn(java,['--enable-native-access=ALL-UNNAMED','-cp',path.join('build','classes')+path.delimiter+'lib/*','payroll.Main','--dashboard','--port',String(port),'--db',database],{windowsHide:true,stdio:['ignore','pipe','pipe']});
    let serverLog='';child.stdout.on('data',data=>serverLog+=data);child.stderr.on('data',data=>serverLog+=data);
    let cookie='',calls=[],download,printCount=0;
    try{
        let ready=false;
        for(let attempt=0;attempt<100;attempt++){try{const r=await fetch(base+'/api/auth/session');if(r.ok){ready=true;break;}}catch{}if(child.exitCode!==null)throw Error(serverLog);await wait(100);}
        assert.ok(ready,'Temporary Java server started: '+serverLog);
        const html=await (await fetch(base+'/')).text();
        const dom=createDom(html),$=dom.get;
        const media={matches:false,addEventListener(){}};
        const browserFetch=async(url,options={})=>{
            calls.push({url,method:options.method??'GET'});
            const response=await fetch(base+url,{...options,headers:{...options.headers,...(cookie?{Cookie:cookie}:{})}});
            for(const value of response.headers.getSetCookie())if(value.startsWith('payroll_session='))cookie=value.split(';')[0];
            return response;
        };
        const sandbox={console,document:dom.document,fetch:browserFetch,AbortController,URLSearchParams,Blob,setTimeout,clearTimeout,
            URL:{createObjectURL(blob){download=blob;return 'blob:test';},revokeObjectURL(){}},
            matchMedia:()=>media,localStorage:{getItem(){return null;},setItem(){}},window:{print(){printCount++;}}};
        vm.createContext(sandbox);
        vm.runInContext(await (await fetch(base+'/payroll-api.js')).text(),sandbox);
        vm.runInContext(await (await fetch(base+'/dashboard.js')).text(),sandbox);
        await until(()=>$('auth-submit').disabled===false);
        assert.match($('auth-title').textContent,/Create your workspace/);
        $('auth-username').value='testadmin';$('auth-password').value='integration-password-123';
        await $('password-toggle').click();assert.equal($('auth-password').type,'text');await $('password-toggle').click();assert.equal($('auth-password').type,'password');
        await $('auth-form').fire('submit');
        assert.equal($('auth-screen').hidden,true);assert.equal($('workspace-content').hidden,false);assert.equal($('count').textContent,0);
        $('create-employee_name').value='Integration Employee';$('create-monthly_salary').value='5000';$('create-bonus').value='250';$('create-tax_rate').value='20';
        await $('create-form').fire('input');assert.match($('create-preview').innerHTML,/4,200\.00/);
        await $('create-form').fire('submit');
        assert.equal($('count').textContent,1);assert.match($('notice').textContent,/saved/);assert.equal($('create-employee_name').value,'');
        const id=vm.runInContext('records[0].id',sandbox);
        const before=calls.length;await $('refresh').click();assert.ok(calls.length>before);assert.match($('notice').textContent,/refreshed/);assert.match($('last-refreshed').textContent,/Updated/);assert.equal($('refresh').disabled,false);
        $('search').value='notfound';await $('search').fire('input');assert.equal($('count').textContent,0);await $('clear').click();assert.equal($('count').textContent,1);
        const action=async name=>{assert.match($('records').innerHTML,new RegExp('data-action="'+name+'"'));await $('records').fire('click',{target:{closest(){return {dataset:{id:String(id),action:name}};}}});};
        await action('edit');assert.equal($('edit-dialog').open,true);$('edit-monthly_salary').value='6000';await $('edit-form').fire('input');await $('edit-form').fire('submit');
        assert.equal($('edit-dialog').open,false);assert.equal($('net-total').textContent,'5,000.00');
        await action('slip');assert.equal($('slip-dialog').open,true);assert.match($('slip-content').innerHTML,/5,000\.00/);await $('print').click();assert.equal(printCount,1);
        await $('export').click();assert.match(await download.text(),/Integration Employee/);
        await $('settings-open').click();assert.equal($('settings-dialog').open,true);$('appearance').value='dark';await $('appearance').fire('change');assert.equal(dom.document.documentElement.dataset.theme,'dark');await $('theme').click();assert.equal(dom.document.documentElement.dataset.theme,'light');
        const settingsClose=dom.closeButtons.find(button=>button.dataset.close==='settings-dialog');await settingsClose.click();assert.equal($('settings-dialog').open,false);
        for(const link of dom.navigation)await link.fire('click');
        await $('menu-toggle').click();assert.equal($('sidebar-backdrop').hidden,false);await $('sidebar-backdrop').click();assert.equal($('sidebar-backdrop').hidden,true);
        await action('delete');assert.equal($('delete-dialog').open,true);const cancelDelete=dom.closeButtons.find(button=>button.dataset.close==='delete-dialog');await cancelDelete.click();assert.equal($('count').textContent,1);
        await action('delete');await $('confirm-delete').click();assert.equal($('count').textContent,0);assert.match($('notice').textContent,/deleted/);
        await $('account-open').click();assert.equal($('account-dialog').open,true);await $('sign-out').click();assert.equal($('workspace-content').hidden,true);assert.equal($('auth-screen').hidden,false);
        assert.equal((await browserFetch('/api/payroll')).status,401);
        $('auth-username').value='testadmin';$('auth-password').value='wrong-password';await $('auth-form').fire('submit');assert.match($('auth-message').textContent,/Incorrect/);
        $('auth-password').value='integration-password-123';await $('auth-form').fire('submit');assert.equal($('auth-screen').hidden,true);assert.equal($('account-username').textContent,'testadmin');
        console.log('Page event integration passed against Java: setup, login/logout, password visibility, create, Refresh, search/clear, edit, payslip/print, export, Settings, themes, navigation, delete cancel/confirm, and protected API access.');
    }catch(error){console.error('Page test failure:',error);throw error;}finally{
        if(child.exitCode===null){const closed=new Promise(resolve=>child.once('close',resolve));child.kill();await closed;}
        for(const name of ['test.db','test.db-shm','test.db-wal','test.db-journal']){const file=path.join(directory,name);if(fs.existsSync(file))fs.unlinkSync(file);}
        fs.rmdirSync(directory);
    }
})().catch(error=>{console.error(error);process.exitCode=1;});

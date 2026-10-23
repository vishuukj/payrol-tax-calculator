/* A strict markup-driven DOM test double. Unknown IDs fail instead of being invented. */
const assert = require('node:assert/strict');
function createDom(html) {
    const elements = new Map(), listeners = new Map();
    let document;
    function attrs(tag) {
        const result = {};
        for (const match of tag.matchAll(/([\w:-]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+)))?/g))
            result[match[1]] = match[2] ?? match[3] ?? match[4] ?? '';
        return result;
    }
    function create(attributes = {}, tagName = 'div') {
        const events = new Map();
        const node = { attributes, tagName:tagName.toUpperCase(), dataset:{}, hidden:'hidden' in attributes, disabled:'disabled' in attributes, checked:'checked' in attributes,
            value:attributes.value ?? '', defaultValue:attributes.value ?? '', type:attributes.type ?? 'text', _html:'', textContent:'', focused:false,
            getAttribute(name) { return this.attributes[name] ?? null; },
            setAttribute(name,value) { this.attributes[name]=String(value); },
            removeAttribute(name) { delete this.attributes[name]; },
            addEventListener(type,handler) { if(!events.has(type))events.set(type,[]); events.get(type).push(handler); },
            async fire(type,extra={}) { const event={target:this,key:'',preventDefault(){this.defaultPrevented=true;},...extra}; if(this['on'+type])await this['on'+type](event);for(const handler of events.get(type)??[])await handler(event); },
            click() { if(!this.disabled)return this.fire('click'); },
            focus() { this.focused=true; document.activeElement=this; },
            showModal() { this.open=true; }, close() {this.open=false;},
            reportValidity() {return true;},
            reset() {for(const input of this.querySelectorAll('input'))input.value=input.defaultValue;},
            querySelectorAll(selector) { if(selector==='input'){const prefix=this.attributes.id?.split('-')[0]; return [...elements.values()].filter(item=>item.tagName==='INPUT'&&item.attributes.id.startsWith(prefix+'-'));}if(selector==='a,button:not([disabled])')return navigation;return [];},
            querySelector(selector) {if(selector==='[type=submit]')return this.submitButton ?? (this.submitButton=create({type:'submit'},'button'));if(selector==='[aria-invalid=true]')return this.querySelectorAll('input').find(input=>input.attributes['aria-invalid']==='true');if(selector==='a')return navigation[0];return null;}
        };
        const classes = new Set((attributes.class??'').split(/\s+/).filter(Boolean));
        let value = node.value;
        Object.defineProperty(node,'value',{get(){return value;},set(next){value=String(next);}});
        node.classList={contains:name=>classes.has(name),add:name=>classes.add(name),remove:name=>classes.delete(name),toggle(name,enabled){if(enabled??!classes.has(name))classes.add(name);else classes.delete(name);}};
        Object.defineProperty(node,'innerHTML',{get(){return this._html;},set(value){this._html=value;parse(value);}});
        for(const [key,value] of Object.entries(attributes))if(key.startsWith('data-'))node.dataset[key.slice(5).replace(/-([a-z])/g,(_,letter)=>letter.toUpperCase())]=value;
        return node;
    }
    function parse(source) {
        for(const match of source.matchAll(/<([a-z][\w-]*)\b[^>]*>/gi)) {
            const attributes=attrs(match[0]);
            if(attributes.id&&!elements.has(attributes.id))elements.set(attributes.id,create(attributes,match[1]));
        }
    }
    parse(html);
    function get(id){assert.ok(elements.has(id),'Markup is missing #'+id);return elements.get(id);}
    get('sort').value='newest';get('chart-mode').value='employees';
    const closeButtons=[...html.matchAll(/<button\b[^>]*data-close="([^"]+)"[^>]*>/g)].map(match=>create(attrs(match[0]),'button'));
    const navMarkup=html.match(/<nav class="sidebar-nav"[\s\S]*?<\/nav>/)[0];
    const navigation=[...navMarkup.matchAll(/<a\b[^>]+>/g)].map(match=>create(attrs(match[0]),'a'));
    const brand=create({href:'#overview',class:'brand'},'a');
    document={activeElement:null,documentElement:{dataset:{}},getElementById:get,addEventListener(type,handler){listeners.set(type,handler);},
        querySelectorAll(selector){if(selector==='[data-close]')return closeButtons;if(selector==='dialog[open]')return [...elements.values()].filter(item=>item.tagName==='DIALOG'&&item.open);if(selector==='.sidebar-nav a')return navigation;if(selector==='.sidebar-nav a, .sidebar .brand')return [...navigation,brand];return [];},
        createElement:tag=>create({},tag)};
    return {document,get,navigation,closeButtons,create};
}
module.exports={createDom};

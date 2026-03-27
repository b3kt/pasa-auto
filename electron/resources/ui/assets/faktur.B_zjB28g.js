import{j as pe,l as fe,s as I,n as M,ap as ge,_ as he,aq as be,p as V,Q as A,h as D,o as k,f as g,e as l,w as i,y as j,g as f,t as p,A as B,ar as Y,a as $,z as q,J as X,K as te,ai as le,am as ie,an as oe,I as se,x as Z,ag as ye,ao as we,L as xe,ak as ke}from"./index.Ba1QpUQ0.js";import{b as de,c as T,Q as K}from"./GenericDialog.P_8PEhlS.js";import{a as N,Q as w}from"./GenericTable.BNmLLODN.js";import{a as Q,b as _,c as U}from"./QList.DnxrBvTj.js";import{Q as Ie,C as je}from"./ClosePopup.gTqJm0JN.js";function R(n,b=new WeakMap){if(Object(n)!==n)return n;if(b.has(n))return b.get(n);const o=n instanceof Date?new Date(n):n instanceof RegExp?new RegExp(n.source,n.flags):n instanceof Set?new Set:n instanceof Map?new Map:typeof n.constructor!="function"?Object.create(null):n.prototype!==void 0&&typeof n.prototype.constructor=="function"?n:new n.constructor;if(typeof n.constructor=="function"&&typeof n.valueOf=="function"){const m=n.valueOf();if(Object(m)!==m){const h=new n.constructor(m);return b.set(n,h),h}}return b.set(n,o),n instanceof Set?n.forEach(m=>{o.add(R(m,b))}):n instanceof Map&&n.forEach((m,h)=>{o.set(h,R(m,b))}),Object.assign(o,...Object.keys(n).map(m=>({[m]:R(n[m],b)})))}const ee=pe({name:"QPopupEdit",props:{modelValue:{required:!0},title:String,buttons:Boolean,labelSet:String,labelCancel:String,color:{type:String,default:"primary"},validate:{type:Function,default:()=>!0},autoSave:Boolean,cover:{type:Boolean,default:!0},disable:Boolean},emits:["update:modelValue","save","cancel","beforeShow","show","beforeHide","hide"],setup(n,{slots:b,emit:o}){const{proxy:m}=fe(),{$q:h}=m,v=I(null),r=I(""),d=I("");let c=!1;const C=M(()=>ge({initialValue:r.value,validate:n.validate,set:y,cancel:J,updatePosition:O},"value",()=>d.value,x=>{d.value=x}));function y(){n.validate(d.value)!==!1&&(S()===!0&&(o("save",d.value,r.value),o("update:modelValue",d.value)),P())}function J(){S()===!0&&o("cancel",d.value,r.value),P()}function O(){he(()=>{v.value.updatePosition()})}function S(){return be(d.value,r.value)===!1}function P(){c=!0,v.value.hide()}function L(){c=!1,r.value=R(n.modelValue),d.value=R(n.modelValue),o("beforeShow")}function H(){o("show")}function F(){c===!1&&S()===!0&&(n.autoSave===!0&&n.validate(d.value)===!0?(o("save",d.value,r.value),o("update:modelValue",d.value)):o("cancel",d.value,r.value)),o("beforeHide")}function W(){o("hide")}function G(){const x=b.default!==void 0?[].concat(b.default(C.value)):[];return n.title&&x.unshift(V("div",{class:"q-dialog__title q-mt-sm q-mb-sm"},n.title)),n.buttons===!0&&x.push(V("div",{class:"q-popup-edit__buttons row justify-center no-wrap"},[V(A,{flat:!0,color:n.color,label:n.labelCancel||h.lang.label.cancel,onClick:J}),V(A,{flat:!0,color:n.color,label:n.labelSet||h.lang.label.set,onClick:y})])),x}return Object.assign(m,{set:y,cancel:J,show(x){v.value?.show(x)},hide(x){v.value?.hide(x)},updatePosition:O}),()=>{if(n.disable!==!0)return V(Ie,{ref:v,class:"q-popup-edit",cover:n.cover,onBeforeShow:L,onShow:H,onBeforeHide:F,onHide:W,onEscapeKey:J},G)}}}),Je={class:"row q-col-gutter-md q-mb-md q-pl-md q-py-md"},Ae={class:"col-12"},Se={class:"text-weight-bold"},Ne={class:"col-12"},Be={class:"text-weight-bold"},De={__name:"SPKDetailsEditor",props:{details:{type:Array,required:!0},allJasaOptions:{type:Array,default:()=>[]},allBarangOptions:{type:Array,default:()=>[]},canEdit:{type:Boolean,default:!0},noSpk:{type:String,default:""}},emits:["update:details","update-master-jasa","update-master-barang"],setup(n,{emit:b}){const o=n,m=b,h=I(!1),v=I(null),r=I({item:null,jumlah:1}),d=I({item:null,jumlah:1}),c=I([]),C=I([]),y=t=>t?new Intl.NumberFormat("id-ID",{style:"currency",currency:"IDR",minimumFractionDigits:0}).format(t):"Rp 0",J=[{name:"no",label:"No",align:"left",field:"no",autoWidth:!0},{name:"namaJasa",label:"Jasa",align:"left",field:"namaItem",autoWidth:!1},{name:"harga",label:"Biaya",align:"right",field:"harga",autoWidth:!0,minWidth:"200px"},{name:"actions",label:"",align:"center",autoWidth:!0}],O=[{name:"no",label:"NO",align:"left",field:"no",autoWidth:!0},{name:"namaBarang",label:"Barang",align:"left",field:"namaItem"},{name:"harga",label:"Harga",align:"right",field:"harga"},{name:"jumlah",label:"Jumlah",align:"center",field:"jumlah"},{name:"actions",label:"",align:"center",autoWidth:!0}],S=M(()=>o.details.filter(t=>t.jasaId).map(t=>{const a=o.allJasaOptions.find(z=>z.id===t.jasaId),e=t.harga!==void 0&&t.harga!==null?t.harga:a?a.hargaJasa:0,s=a?a.hargaJasa:0,u=a?a.namaJasa:t.namaItem||"Unknown Service";return{...t,harga:e,hargaMaster:s,namaItem:u}})),P=M(()=>o.details.filter(t=>t.sparepartId).map(t=>{const a=o.allBarangOptions.find(z=>z.id===t.sparepartId),e=t.harga!==void 0&&t.harga!==null?t.harga:a?a.hargaJual:0,s=a?a.hargaJual:0,u=a?a.namaBarang:t.namaItem||"Unknown Part";return{...t,harga:e,hargaMaster:s,namaItem:u}})),L=M(()=>S.value.reduce((t,a)=>t+a.harga*a.jumlah,0)),H=M(()=>P.value.reduce((t,a)=>t+a.harga*a.jumlah,0)),F=(t,a)=>{a(()=>{if(t==="")c.value=o.allJasaOptions;else{const e=t.toLowerCase();c.value=o.allJasaOptions.filter(s=>s.namaJasa.toLowerCase().indexOf(e)>-1)}})},W=(t,a)=>{a(()=>{if(t==="")C.value=o.allBarangOptions;else{const e=t.toLowerCase();C.value=o.allBarangOptions.filter(s=>s.namaBarang.toLowerCase().indexOf(e)>-1)}})},G=t=>o.details.some(a=>a.jasaId===t.id),x=t=>o.details.some(a=>a.sparepartId===t.id),re=()=>{if(!r.value.item)return;const t={id:{noSpk:o.noSpk,namaJasa:r.value.item.namaJasa},namaItem:r.value.item.namaJasa,jasaId:r.value.item.id,harga:r.value.item.hargaJasa,hargaMaster:r.value.item.hargaJasa,jumlah:r.value.jumlah,keterangan:"",tempId:Date.now()},a=[...o.details,t];m("update:details",a),r.value={item:null,jumlah:1}},ae=(t,a)=>{if(t.jasaId){const e=o.allJasaOptions.find(s=>s.id===t.jasaId);e&&e.hargaJasa!==a?(v.value={row:t,newValue:a,master:e,type:"jasa"},h.value=!0):E(t,a)}else if(t.sparepartId){const e=o.allBarangOptions.find(s=>s.id===t.sparepartId);e&&e.hargaJual!==a?(v.value={row:t,newValue:a,master:e,type:"barang"},h.value=!0):E(t,a)}},E=(t,a)=>{const e=[...o.details],s=e.findIndex(u=>u.tempId&&u.tempId===t.tempId||u.id&&t.id&&u.id.namaJasa===t.id.namaJasa&&u.id.noSpk===t.id.noSpk);s>-1&&(e[s]={...e[s],harga:a},m("update:details",e))},ue=(t,a)=>{const e=[...o.details],s=e.findIndex(u=>u.tempId&&u.tempId===t.tempId||u.id&&t.id&&u.id.namaJasa===t.id.namaJasa&&u.id.noSpk===t.id.noSpk);s>-1&&(e[s]={...e[s],jumlah:a},m("update:details",e))},me=()=>{if(v.value){const{row:t,newValue:a,master:e,type:s}=v.value;E(t,a),s==="jasa"?m("update-master-jasa",{...e,hargaJasa:a}):s==="barang"&&m("update-master-barang",{...e,hargaJual:a}),h.value=!1,v.value=null}},ce=()=>{if(v.value){const{row:t,newValue:a}=v.value;E(t,a),h.value=!1,v.value=null}},ve=()=>{if(!d.value.item)return;const t={id:{noSpk:o.noSpk,namaJasa:d.value.item.namaBarang},namaItem:d.value.item.namaBarang,sparepartId:d.value.item.id,harga:d.value.item.hargaJual,hargaMaster:d.value.item.hargaJual,jumlah:d.value.jumlah,keterangan:"",tempId:Date.now()},a=[...o.details,t];m("update:details",a),d.value={item:null,jumlah:1}},ne=t=>{const a=o.details.findIndex(e=>e.tempId&&e.tempId===t.tempId||e.id&&t.id&&e.id.namaJasa===t.id.namaJasa&&e.id.noSpk===t.id.noSpk);if(a>-1){const e=[...o.details];e.splice(a,1),m("update:details",e)}};return(t,a)=>(k(),D(X,null,[g("div",Je,[g("div",Ae,[l(Z,{flat:"",bordered:"",class:"full-height"},{default:i(()=>[l(j,{class:"bg-grey-2 q-py-xs"},{default:i(()=>[...a[4]||(a[4]=[g("div",{class:"text-subtitle2"},"DAFTAR LAYANAN PERBAIKAN / SERVIS",-1)])]),_:1}),l(j,{class:"q-pa-none"},{default:i(()=>[l(de,{flat:"",rows:S.value,columns:J,"row-key":"tempId",dense:"","hide-pagination":"","rows-per-page-options":[0],separator:"cell"},{header:i(()=>[l(N,null,{default:i(()=>[(k(),D(X,null,te(J,e=>l(T,{key:e.name,align:e.align,"auto-width":e.autoWidth,style:le("width:"+e.minWidth)},{default:i(()=>[f(p(e.label),1)]),_:2},1032,["align","auto-width","style"])),64))]),_:1}),n.canEdit?(k(),$(N,{key:0},{default:i(()=>[l(T,{class:"text-center",colspan:"4"},{default:i(()=>[l(K,{modelValue:r.value.item,"onUpdate:modelValue":[a[0]||(a[0]=e=>r.value.item=e),re],options:c.value,"option-label":"namaJasa",dense:"",outlined:"",label:"Pilih Jasa","use-input":"","input-debounce":"300",onFilter:F,"emit-value":"","map-options":"","option-disable":G},{option:i(e=>[l(Q,ie(oe(e.itemProps)),{default:i(()=>[l(_,null,{default:i(()=>[l(U,null,{default:i(()=>[f(p(e.opt.namaJasa),1)]),_:2},1024),l(U,{caption:""},{default:i(()=>[f(p(y(e.opt.hargaJasa)),1)]),_:2},1024)]),_:2},1024)]),_:2},1040)]),_:1},8,["modelValue","options"])]),_:1})]),_:1})):q("",!0)]),body:i(e=>[l(N,{props:e},{default:i(()=>[l(w,{key:"no",props:e},{default:i(()=>[f(p(e.rowIndex+1),1)]),_:2},1032,["props"]),l(w,{key:"namaJasa",props:e},{default:i(()=>[f(p(e.row.namaItem),1)]),_:2},1032,["props"]),l(w,{key:"harga",props:e,class:"text-right"},{default:i(()=>[f(p(y(e.row.harga))+" ",1),l(ee,{"model-value":e.row.harga,"auto-save":"",onSave:s=>ae(e.row,s)},{default:i(s=>[l(B,{modelValue:s.value,"onUpdate:modelValue":u=>s.value=u,modelModifiers:{number:!0},dense:"",outlined:"",autofocus:"",counter:"",onKeyup:Y(s.set,["enter"]),type:"number",rules:[u=>u>0||"Harga harus lebih dari 0"]},null,8,["modelValue","onUpdate:modelValue","onKeyup","rules"])]),_:1},8,["model-value","onSave"])]),_:2},1032,["props"]),l(w,{key:"jumlah",props:e},{default:i(()=>[f(p(e.row.jumlah),1)]),_:2},1032,["props"]),l(w,{key:"total",props:e,class:"text-right"},{default:i(()=>[f(p(y(e.row.harga*e.row.jumlah)),1)]),_:2},1032,["props"]),l(w,{key:"actions",props:e,class:"text-center"},{default:i(()=>[n.canEdit?(k(),$(A,{key:0,flat:"",dense:"",round:"",icon:"delete",color:"negative",size:"sm",onClick:s=>ne(e.row)},null,8,["onClick"])):q("",!0)]),_:2},1032,["props"])]),_:2},1032,["props"])]),_:1},8,["rows"])]),_:1}),l(se),l(j,{class:"q-py-xs text-right bg-grey-2"},{default:i(()=>[g("span",Se,"Total: "+p(y(L.value)),1)]),_:1})]),_:1})]),g("div",Ne,[l(Z,{flat:"",bordered:"",class:"full-height"},{default:i(()=>[l(j,{class:"bg-grey-2 q-py-xs"},{default:i(()=>[...a[5]||(a[5]=[g("div",{class:"text-subtitle2"},"BARANG / SPAREPART",-1)])]),_:1}),l(j,{class:"q-pa-none"},{default:i(()=>[l(de,{flat:"",rows:P.value,columns:O,"row-key":"tempId",dense:"","hide-pagination":"","rows-per-page-options":[0],separator:"cell"},{header:i(()=>[l(N,null,{default:i(()=>[(k(),D(X,null,te(O,e=>l(T,{key:e.name,align:e.align,"auto-width":e.autoWidth,style:le("width:"+e.minWidth)},{default:i(()=>[f(p(e.label),1)]),_:2},1032,["align","auto-width","style"])),64))]),_:1}),n.canEdit?(k(),$(N,{key:0},{default:i(()=>[l(T,{class:"text-center",colspan:"6"},{default:i(()=>[l(K,{modelValue:d.value.item,"onUpdate:modelValue":[a[1]||(a[1]=e=>d.value.item=e),ve],options:C.value,"option-label":"namaBarang",dense:"",outlined:"",label:"Pilih Barang","use-input":"","input-debounce":"300",onFilter:W,"emit-value":"","map-options":"","option-disable":x},{option:i(e=>[l(Q,ie(oe(e.itemProps)),{default:i(()=>[l(_,null,{default:i(()=>[l(U,null,{default:i(()=>[f(p(e.opt.namaBarang),1)]),_:2},1024),l(U,{caption:""},{default:i(()=>[f(p(y(e.opt.hargaJual)),1)]),_:2},1024)]),_:2},1024)]),_:2},1040)]),_:1},8,["modelValue","options"])]),_:1})]),_:1})):q("",!0)]),body:i(e=>[l(N,{props:e},{default:i(()=>[l(w,{key:"no",props:e},{default:i(()=>[f(p(e.rowIndex+1),1)]),_:2},1032,["props"]),l(w,{key:"namaBarang",props:e},{default:i(()=>[f(p(e.row.namaItem),1)]),_:2},1032,["props"]),l(w,{key:"harga",props:e,class:"text-right"},{default:i(()=>[f(p(y(e.row.harga))+" ",1),l(ee,{"model-value":e.row.harga,"auto-save":"",onSave:s=>ae(e.row,s)},{default:i(s=>[l(B,{modelValue:s.value,"onUpdate:modelValue":u=>s.value=u,modelModifiers:{number:!0},dense:"",outlined:"",autofocus:"",counter:"",onKeyup:Y(s.set,["enter"]),type:"number",label:"Harga Barang Satuan",rules:[u=>u>0||"Harga harus lebih dari 0"]},null,8,["modelValue","onUpdate:modelValue","onKeyup","rules"])]),_:1},8,["model-value","onSave"])]),_:2},1032,["props"]),l(w,{key:"jumlah",props:e},{default:i(()=>[f(p(e.row.jumlah)+" ",1),n.canEdit?(k(),$(ee,{key:0,"model-value":e.row.jumlah,"auto-save":"",onSave:s=>ue(e.row,s)},{default:i(s=>[l(B,{modelValue:s.value,"onUpdate:modelValue":u=>s.value=u,modelModifiers:{number:!0},type:"number",dense:"",outlined:"",autofocus:"",counter:"",rules:[u=>u>0||"Jumlah harus lebih dari 0"],onKeyup:Y(s.set,["enter"]),label:"Jumlah barang"},null,8,["modelValue","onUpdate:modelValue","rules","onKeyup"])]),_:1},8,["model-value","onSave"])):q("",!0)]),_:2},1032,["props"]),l(w,{key:"total",props:e,class:"text-right"},{default:i(()=>[f(p(y(e.row.harga*e.row.jumlah)),1)]),_:2},1032,["props"]),l(w,{key:"actions",props:e,class:"text-center"},{default:i(()=>[n.canEdit?(k(),$(A,{key:0,flat:"",dense:"",round:"",icon:"delete",color:"negative",size:"sm",onClick:s=>ne(e.row)},null,8,["onClick"])):q("",!0)]),_:2},1032,["props"])]),_:2},1032,["props"])]),_:1},8,["rows"])]),_:1}),l(se),l(j,{class:"q-py-xs text-right bg-grey-2"},{default:i(()=>[g("span",Be,"Total: "+p(y(H.value)),1)]),_:1})]),_:1})])]),l(ke,{modelValue:h.value,"onUpdate:modelValue":a[3]||(a[3]=e=>h.value=e),persistent:""},{default:i(()=>[l(Z,{style:{"min-width":"350px"}},{default:i(()=>[l(j,{class:"row items-center"},{default:i(()=>[l(ye,{icon:"warning",color:"warning","text-color":"white"}),a[6]||(a[6]=g("span",{class:"q-ml-sm text-h6"},"Update Master Data?",-1))]),_:1}),l(j,{class:"q-pt-none"},{default:i(()=>[a[7]||(a[7]=f(" Harga master untuk ",-1)),g("strong",null,'"'+p(v.value?.row.namaItem)+'"',1),f(" berbeda ("+p(y(v.value?.type==="jasa"?v.value?.master.hargaJasa:v.value?.master.hargaJual))+"). Apakah Anda ingin memperbarui harga master menjadi ",1),g("strong",null,p(y(v.value?.newValue)),1),a[8]||(a[8]=f(" juga? ",-1))]),_:1}),l(we,{align:"right",class:"text-primary"},{default:i(()=>[xe(l(A,{flat:"",label:"Batal",onClick:a[2]||(a[2]=e=>v.value=null)},null,512),[[je]]),l(A,{flat:"",label:"Hanya Item Ini",onClick:ce}),l(A,{color:"primary",label:"Update Keduanya",onClick:me})]),_:1})]),_:1})]),_:1},8,["modelValue"])],64))}},Ce={class:"q-mb-md"},Oe={class:"q-mb-md"},Pe={class:"q-mb-md"},Ve={class:"row q-col-gutter-sm"},$e={class:"q-mb-md col-6"},qe={class:"q-mb-md col-6"},Ke={__name:"SPKCustomerInfo",props:{namaPelanggan:String,alamat:String,nopol:String,merk:String,jenis:String,isNewCustomer:Boolean,merkOptions:Array,filteredJenisOptions:Array,loadingMerk:Boolean,loadingJenis:Boolean},emits:["update:namaPelanggan","update:alamat","update:merk","update:jenis","filter:merk","filter:jenis","check:vehicle"],setup(n,{emit:b}){const o=n,m=b,h=r=>{m("update:merk",r),o.isNewCustomer&&r&&o.jenis&&m("check:vehicle",r,o.jenis)},v=r=>{m("update:jenis",r),o.isNewCustomer&&o.merk&&r&&m("check:vehicle",o.merk,r)};return(r,d)=>(k(),D("div",null,[d[4]||(d[4]=g("div",{class:"q-mb-md"},[g("span",{class:"text-caption text-bold"},"Informasi Pelanggan")],-1)),g("div",Ce,[l(B,{"model-value":n.namaPelanggan,label:"Nama *",outlined:"",dense:"","onUpdate:modelValue":d[0]||(d[0]=c=>r.$emit("update:namaPelanggan",c)),readonly:!n.isNewCustomer,rules:n.isNewCustomer?[c=>!!c||"Nama harus diisi"]:[]},null,8,["model-value","readonly","rules"])]),g("div",Oe,[l(B,{"model-value":n.alamat,label:"Alamat",outlined:"",dense:"","onUpdate:modelValue":d[1]||(d[1]=c=>r.$emit("update:alamat",c)),readonly:!n.isNewCustomer,type:"textarea",rows:"4"},null,8,["model-value","readonly"])]),g("div",Pe,[l(B,{"model-value":n.nopol,label:"Kendaraan",outlined:"",dense:"",readonly:""},null,8,["model-value"])]),g("div",Ve,[g("div",$e,[l(K,{"model-value":n.merk,label:"Merk *",outlined:"",dense:"","onUpdate:modelValue":h,options:n.merkOptions,"option-label":c=>c,"option-value":"merk","emit-value":"","map-options":"","use-input":"","input-debounce":"300",onFilter:r.filterMerk,loading:n.loadingMerk,readonly:!n.isNewCustomer,rules:n.isNewCustomer?[c=>!!c||"Merk harus diisi"]:[],"new-value-mode":"add-unique","hide-bottom-space":""},{"no-option":i(()=>[l(Q,null,{default:i(()=>[l(_,{class:"text-grey"},{default:i(()=>[...d[2]||(d[2]=[f(" No results found. Type to add new merk. ",-1)])]),_:1})]),_:1})]),_:1},8,["model-value","options","option-label","onFilter","loading","readonly","rules"])]),g("div",qe,[l(K,{"model-value":n.jenis,label:"Jenis",outlined:"",dense:"","onUpdate:modelValue":v,options:n.filteredJenisOptions,"option-label":c=>c,"option-value":"jenis","emit-value":"","map-options":"","use-input":"","input-debounce":"300",onFilter:r.filterJenis,loading:n.loadingJenis,readonly:!n.isNewCustomer,"new-value-mode":"add-unique","hide-bottom-space":""},{"no-option":i(()=>[l(Q,null,{default:i(()=>[l(_,{class:"text-grey"},{default:i(()=>[...d[3]||(d[3]=[f(" No results found. Type to add new jenis. ",-1)])]),_:1})]),_:1})]),_:1},8,["model-value","options","option-label","onFilter","loading","readonly"])])])]))}},Qe=`<html>

<head>
    <title>Print Penjualan \${data.noPenjualan}</title>
    <style>
        @page {
            size: letter;
            margin: 0.5cm;
        }

        body,
        table,
        td,
        th {
            font-family: 'Courier New', Courier, monospace;
            font-size: 10pt;
            font-weight: bold;
            margin: 0;
            padding: 2px;
            color: #000;
        }

        .container {
            width: 100%;
            max-width: 21cm;
            margin: 0 auto;
        }

        .header {
            display: flex;
            justify-content: space-between;
            margin-bottom: 10px;
        }

        .company-info {
            width: 60%;
        }

        .company-name {
            font-size: 14pt;
            font-weight: bold;
            margin-bottom: 2px;
        }

        .invoice-title {
            width: 40%;
            text-align: right;
        }

        .invoice-header {
            font-size: 11pt;
            font-weight: bold;
            border: 1px solid #000;
            padding: 5px 15px;
            display: inline-block;
        }

        .info-section {
            display: flex;
            margin-bottom: 5px;
            border-top: 1px double #000;
            border-bottom: 1px double #000;
            padding: 5px 0;
        }

        .info-left {
            width: 55%;
        }

        .info-right {
            width: 45%;
        }

        .info-row {
            display: flex;
            margin-bottom: 1px;
        }

        .label {
            width: 120px;
        }

        .separator {
            width: 15px;
        }

        .value {
            flex: 1;
        }

        .items-table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 5px;
        }

        .items-table th {
            border-bottom: 1px solid #000;
            padding: 3px 2px;
            text-align: left;
            font-weight: bold;
        }

        .items-table td {
            padding: 2px 2px;
            vertical-align: top;
        }

        .text-right {
            text-align: right !important;
        }

        .text-center {
            text-align: center !important;
        }

        .footer-section {
            display: flex;
            margin-top: 5px;
            border-top: 1px solid #000;
            padding-top: 5px;
        }

        .footer-left {
            width: 65%;
            font-size: 10pt;
        }

        .footer-right {
            width: 35%;
        }

        .total-row {
            display: flex;
            justify-content: space-between;
            margin-bottom: 1px;
        }

        .grand-total {
            font-weight: bold;
            border-top: 1px solid #000;
            border-bottom: 1px solid #000;
            padding: 3px 0;
            margin: 3px 0;
        }

        .signatures {
            display: flex;
            justify-content: space-between;
            margin-top: 30px;
            text-align: center;
        }

        .sig-box {
            width: 30%;
        }

        .sig-line {
            margin-top: 40px;
        }

        .uppercase {
            text-transform: uppercase;
        }

        .group-header {
            font-weight: bold;
            font-style: italic;
        }
    </style>
</head>

<body>
    <div class="container">
        <div class="header">
            <div class="company-info">
                <div class="company-name">PASA AUTO</div>
                <div>JL. REGENSI 2 RUKO WISMA HARAPAN BLOK D5 NO. 25,</div>
                <div>GEMBOR, PERIUK, KOTA TANGERANG</div>
                <div>TLP: 0813 8101 4647</div>
            </div>
            <div class="invoice-title">
                <div class="invoice-header">FAKTUR PENJUALAN</div>
            </div>
        </div>

        <div class="info-section">
            <div class="info-left">
                <!--
                <div class="info-row">
                    <div class="label">Kepada YTH,</div>
                </div>
                -->
                <div class="info-row">
                    <div class="label">NAMA PEMILIK</div>
                    <div class="separator">:</div>
                    <div class="value uppercase">\${data.namaPelanggan || 'TUNAI'}</div>
                </div>
                <div class="info-row">
                    <div class="label">NO POLISI</div>
                    <div class="separator">:</div>
                    <div class="value uppercase">\${data.nopol || '-'}</div>
                </div>
                <div class="info-row">
                    <div class="label">MERK - JENIS</div>
                    <div class="separator">:</div>
                    <div class="value uppercase">\${data.merk || '-'} \${data.model || ''}</div>
                </div>
                <div class="info-row">
                    <div class="label">ALAMAT</div>
                    <div class="separator">:</div>
                    <div class="value uppercase">\${data.alamatPelanggan || '-'}</div>
                </div>
            </div>
            <div class="info-right">
                <div class="info-row">
                    <div class="label">KM</div>
                    <div class="separator">:</div>
                    <div class="value">\${data.km || '-'}</div>
                </div>
                <div class="info-row">
                    <div class="label">MEKANIK</div>
                    <div class="separator">:</div>
                    <div class="value uppercase">\${data.namaMekanik || '-'}</div>
                </div>
                <div class="info-row">
                    <div class="label">NO FAKTUR</div>
                    <div class="separator">:</div>
                    <div class="value">\${data.noPenjualan}</div>
                </div>
                <div class="info-row">
                    <div class="label">TANGGAL</div>
                    <div class="separator">:</div>
                    <div class="value">\${data.tanggal}</div>
                </div>
            </div>
        </div>

        <table class="items-table">
            <thead>
                <tr>
                    <th></th>
                    <th width="5%">NO</th>
                    <th width="50%">JASA & BARANG</th>
                    <th width="15%" class="text-right">HARGA</th>
                    <th width="10%" class="text-right">QTY</th>
                    <th width="20%" class="text-right">TOTAL</th>
                </tr>
            </thead>
            <tbody>
                \${(() => {
                    const jasaItems = data.items.filter(i => i.type === 'JASA');
                    const barangItems = data.items.filter(i => i.type === 'BARANG');
                    let html = '';
                    let rowNo = 1;

                    if (jasaItems.length > 0) {
                        let firstRow = 'Jasa ';
                        jasaItems.forEach(item => {
                            html += \`
                            <tr>
                                <td class="group-header">\${firstRow}</td>
                                <td>\${rowNo++}</td>
                                <td class="uppercase">\${item.nama}</td>
                                <td class="text-right">\${formatNumber(item.harga)}</td>
                                <td class="text-right">\${item.qty}</td>
                                <td class="text-right">\${formatNumber(item.subTotal)}</td>
                            </tr>\`;
                            firstRow = '';
                        });
                    }

                    if (barangItems.length > 0) {
                        let firstRow = 'Barang ';
                        barangItems.forEach(item => {
                            html += \`
                            <tr>
                                <td class="group-header">\${firstRow}</td>
                                <td>\${rowNo++}</td>
                                <td class="uppercase">\${item.nama}</td>
                                <td class="text-right">\${formatNumber(item.harga)}</td>
                                <td class="text-right">\${item.qty}</td>
                                <td class="text-right">\${formatNumber(item.subTotal)}</td>
                            </tr>\`;
                            firstRow = '';
                        });
                    }
                    return html;
                })()}
            </tbody>
        </table>

        <div class="footer-section">
            <div class="footer-left">
                <br/>
                <div>Diterima</div>
                <div class="sig-line"></div>
                <br/>
                <div>Untuk pembayaran transfer ke <br/>rek. BCA 8820626291 a/n PAIYO</div>
            </div>
            <div class="footer-right">
                <div class="total-row grand-total">
                    <div>Grand Total:</div>
                    <div>\${formatCurrency(data.grandTotal)}</div>
                </div>
                <br/>
                <div class="text-center">
                    <div>Hormat Kami,</div>
                    <div class="sig-line"></div>
                    <div>( PASA AUTO )</div>
                </div>
            </div>
        </div>
    </div>
</body>

</html>`;export{Ke as _,De as a,Qe as f};

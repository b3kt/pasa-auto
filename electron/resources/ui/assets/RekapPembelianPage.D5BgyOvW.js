import{Q as I,b as da,a as he,_ as we}from"./GenericDialog.P_8PEhlS.js";import{s,n as K,G as $,v as ua,a as E,o as x,w as o,e as t,f as i,h as ke,z as O,A as m,g as y,B as xe,t as P,Q as c,al as Se,L as ma,ad as pa,ak as Ve,x as Ae,y as M,I as J,ao as Ie,_ as va}from"./index.Ba1QpUQ0.js";import{Q as ga,a as ca}from"./QPopupProxy.Bt0dAswR.js";import{Q as ba}from"./QBadge.Dmckzz72.js";import{Q as L}from"./QChip.CD0YUiF0.js";import{a as Oe,b as Te}from"./QList.DnxrBvTj.js";import{Q as ae,_ as fa}from"./GenericTable.BNmLLODN.js";import{Q as le}from"./QForm.CFjRHBbv.js";import{Q as ya}from"./QSplitter.C5KFEttO.js";import{Q as Pa}from"./QPage.Bol3KtuZ.js";import{C as ha}from"./ClosePopup.gTqJm0JN.js";import{api as h}from"./axios.Ds4-bm2n.js";import{u as wa}from"./use-quasar.DOkC72yp.js";import{d as H}from"./date.DDVE2uSi.js";import"./format.JINsTmJZ.js";import"./position-engine.Cp9Hx3mv.js";import"./QToolbar.PTRfm_c1.js";import"./syncService.CCZyDCAs.js";const ka=`<html>

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
            font-size: 8pt;
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
            font-size: 8pt;
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
                <div>TLP: 0813 8101 46747</div>
            </div>
            <div class="invoice-title">
                <div class="invoice-header">REKAP PEMBELIAN</div>
            </div>
        </div>

        <div class="info-section">
            <div class="info-left">
                <div class="info-row">
                    <div class="label">TANGGAL</div>
                    <div class="separator">:</div>
                    <div class="value">\${data.tanggal}</div>
                </div>
                <div class="info-row">
                    <div class="label">PERIODE</div>
                    <div class="separator">:</div>
                    <div class="value">\${data.filters.dateRange || '-'}</div>
                </div>
                <div class="info-row">
                    <div class="label">SEARCH</div>
                    <div class="separator">:</div>
                    <div class="value">\${data.filters.search || '-'}</div>
                </div>
            </div>
            <div class="info-right">

                <div class="info-row">
                    <div class="label">JENIS</div>
                    <div class="separator">:</div>
                    <div class="value">\${data.filters.jenisPembelian || '-'}</div>
                </div>
                <div class="info-row">
                    <div class="label">KATEGORI</div>
                    <div class="separator">:</div>
                    <div class="value">\${data.filters.kategoriOperasional || '-'}</div>
                </div>
                <div class="info-row">
                    <div class="label">STATUS</div>
                    <div class="separator">:</div>
                    <div class="value">\${data.filters.status || '-'}</div>
                </div>
            </div>
        </div>

        <table class="items-table">
            <thead>
                <tr>
                    <th width="5%">NO</th>
                    <th width="15%">NO. PEMBELIAN</th>
                    <th width="17%">TANGGAL</th>
                    <th width="12%">JENIS</th>
                    <th width="10%">KATEGORI</th>
                    <th width="15%">SUPPLIER</th>
                    <th width="10%">STATUS</th>
                    <th width="10%" class="text-center">METODE</th>
                    <th width="15%" class="text-right">TOTAL</th>
                </tr>
            </thead>
            <tbody>
                \${(() => {
                    let html = '';
                    let rowNo = 1;
                    
                    data.items.forEach(item => {
                        html += '<tr>';
                        html += '<td>' + rowNo++ + '</td>';
                        html += '<td class="uppercase">' + item.nama + '</td>';
                        html += '<td>' + (item.tanggal || '-') + '</td>';
                        html += '<td class="uppercase">' + item.type + '</td>';
                        html += '<td class="uppercase">' + (item.kategori || '-') + '</td>';
                        html += '<td class="uppercase">' + (item.supplier || '-') + '</td>';
                        html += '<td class="text-center">' + (item.status || '-') + '</td>';
                        html += '<td class="text-center">' + (item.metode || '-') + '</td>';
                        html += '<td class="text-right">' + formatCurrency(item.harga) + '</td>';
                        html += '</tr>';
                    });
                    
                    return html;
                })()}
            </tbody>
        </table>

        <div class="footer-section">
            <div class="footer-left">
                <br/>
                <div class="sig-line"></div>
            </div>
            <div class="footer-right">
                <div class="total-row grand-total">
                    <div>Grand Total:</div>
                    <div>\${formatCurrency(data.grandTotal)}</div>
                </div>
            </div>
        </div>
    </div>
</body>

</html>`,xa={class:"row items-center q-gutter-sm"},Sa={class:"row items-center justify-end q-gutter-sm"},Va={class:"q-pa-md scroll",style:{height:"100%"}},Aa={class:"q-mb-lg"},Ia={class:"row q-col-gutter-sm"},Oa={class:"col-12"},Ta={class:"col-12"},Na={class:"col-12"},Da={key:0,class:"q-mb-lg"},Ea={class:"row q-col-gutter-sm"},Ra={class:"col-6"},_a={class:"col-6"},Ca={key:1,class:"q-mb-lg"},Ua={class:"row q-col-gutter-sm"},ja={class:"col-12"},La={class:"text-primary"},qa={class:"q-mb-lg"},Ba={class:"text-subtitle1 text-weight-bold text-grey-8 q-mb-sm"},Fa={class:"row items-center justify-between"},Qa={class:"bg-grey-2 q-pa-md rounded-borders q-mb-lg"},Ka={class:"row q-col-gutter-sm"},$a={class:"col-12"},Ma={class:"self-center full-width no-outline",tabindex:"0"},Ja={class:"col-12"},Ha={class:"col-12"},Ya={class:"row justify-end q-mt-md q-gutter-sm"},za={class:"text-h6"},Ga={class:"row q-col-gutter-sm"},Wa={class:"col-12"},Xa={class:"col-12"},Za={class:"col-6"},el={class:"col-6"},al={class:"col-12"},ll={class:"self-center full-width no-outline",tabindex:"0"},tl={class:"col-12"},nl={class:"row q-col-gutter-sm"},il={class:"col-12"},ol={class:"col-12"},sl={class:"col-6"},rl={class:"col-6"},dl={class:"col-6"},ul={class:"col-6"},ml={class:"col-6"},pl={class:"col-6"},vl={class:"col-12"},gl={class:"q-pa-sm",style:{height:"70vh",width:"100%"}},cl=["srcdoc"],Ne="pembelian_status_filter",Cl={__name:"RekapPembelianPage",setup(bl){const p=wa(),De=()=>{try{const l=localStorage.getItem(Ne);return l?JSON.parse(l):[]}catch(l){return console.error("Failed to load filter from storage:",l),[]}},Ee=l=>{try{localStorage.setItem(Ne,JSON.stringify(l))}catch(e){console.error("Failed to save filter to storage:",e)}},Y=s(!1),z=s(!1),q=s(!1),T=s(""),S=s(null),V=s(null),w=s(De()),te=new Date,Re=H.subtractFromDate(te,{days:1}),d=s({from:H.formatDate(Re,"YYYY/MM/DD"),to:H.formatDate(te,"YYYY/MM/DD")}),B=s(!1),G=s(""),ne=K(()=>!d.value||!d.value.from&&!d.value.to?"":d.value.from&&d.value.to?`${d.value.from} - ${d.value.to}`:d.value.from?`From: ${d.value.from}`:d.value.to?`To: ${d.value.to}`:""),ie=s([]),R=s(!1),A=s(!1),F=s(null),oe=s(70),se=s(null),_e=s(""),Ce=["SPAREPART","OPERASIONAL","BARANG"],Ue=[{label:"Pembelian Sparepart",value:"SPAREPART"},{label:"Pembelian Barang (Non-Supplier)",value:"BARANG"},{label:"Pengeluaran Operasional",value:"OPERASIONAL"}],re=["DAILY","WEEKLY","MONTHLY","YEARLY","ON_DEMAND"],de=s(["LUNAS","BELUM_LUNAS","DP"]),N=s([]),_=s([]),C=s(""),Q=s(!1),U=s(!1),j=s(!1),W=s(-1),u=s({namaSupplier:"",alamat:"",telepon:"",email:"",kontakPerson:"",noHpKontak:"",kota:"",kodePos:"",keterangan:""}),X=s(!1),r=s({id:null,pembelianId:null,namaItem:"",kategoriItem:"SPAREPART",harga:0,kuantiti:1,total:0,keterangan:""}),je=[{label:"Sparepart",value:"SPAREPART"},{label:"Barang",value:"BARANG"},{label:"Operasional",value:"OPERASIONAL"}],Le=[{name:"namaItem",label:"Nama Item",align:"left",field:"namaItem",sortable:!1},{name:"kategoriItem",label:"Kategori",align:"left",field:"kategoriItem",sortable:!1},{name:"harga",label:"Harga",align:"right",field:"harga",sortable:!1},{name:"kuantiti",label:"Qty",align:"center",field:"kuantiti",sortable:!1},{name:"total",label:"Total",align:"right",field:"total",sortable:!1},{name:"keterangan",label:"Keterangan",align:"left",field:"keterangan",sortable:!1},{name:"actions",label:"Actions",align:"center",field:"actions",sortable:!1}],qe=K(()=>n.value.details?n.value.details.reduce((l,e)=>l+(e.total||0),0):0),v=K(()=>!1),g=s({sortBy:"tanggalPembelian",descending:!0,page:1,rowsPerPage:10,rowsNumber:0}),n=s({id:null,noPembelian:"",tanggalPembelian:"",jenisPembelian:"SPAREPART",jenisOperasional:"",kategoriOperasional:null,supplierId:null,grandTotal:0,statusPembayaran:"BELUM_LUNAS",jenisPembayaran:"CASH",diskon:0,ppn:0,keterangan:"",details:[]}),Z=s({}),Be=K(()=>{if(!A.value)return!1;const l=JSON.stringify(n.value),e=JSON.stringify(Z.value);return l!==e}),Fe=[{name:"tanggalPembelian",label:"Tanggal",align:"left",field:"tanggalPembelian",sortable:!0},{name:"noPembelian",required:!0,label:"No Pembelian",align:"left",field:"noPembelian",sortable:!0},{name:"jenisPembelian",label:"Jenis Pengeluaran",align:"center",field:"jenisPembelian",sortable:!0},{name:"jenisOperasional",label:"Jenis Operasional",align:"left",field:"jenisOperasional",sortable:!0},{name:"kategoriOperasional",label:"Kategori Operasional",align:"left",field:"kategoriOperasional",sortable:!0},{name:"namaSupplier",label:"Supplier",align:"left",field:"namaSupplier",sortable:!0},{name:"grandTotal",label:"Jumlah Biaya",align:"right",field:"grandTotal",sortable:!0},{name:"jenisPembayaran",label:"Jenis Pembayaran",align:"center",field:"jenisPembayaran",sortable:!0},{name:"keterangan",label:"Keterangan",align:"center",field:"keterangan",sortable:!0},{name:"statusPembayaran",label:"Status",align:"center",field:"statusPembayaran",sortable:!0}],k=async(l=g.value)=>{Y.value=!0;try{const e={page:l.page,rowsPerPage:l.rowsPerPage};l.sortBy&&(e.sortBy=l.sortBy,e.descending=l.descending),T.value&&(e.search=T.value),S.value&&(e.jenisPembelian=S.value),V.value&&(e.kategoriOperasional=V.value),w.value&&w.value.length>0&&(e.statusFilter=w.value.join(",")),d.value?.from&&(e.startDate=d.value.from.replace(/\//g,"-")),d.value?.to&&(e.endDate=d.value.to.replace(/\//g,"-"));const a=await h.get("/api/pazaauto/pembelian/paginated",{params:e});if(a.data.success){const b=a.data.data;ie.value=b.rows||[],g.value.rowsNumber=b.rowsNumber,g.value.page=b.page,g.value.rowsPerPage=b.rowsPerPage}}catch(e){p.notify({type:"negative",message:"Failed to fetch pembelian data",caption:e.response?.data?.message||e.message})}finally{Y.value=!1}},Qe=l=>{const{page:e,rowsPerPage:a,sortBy:b,descending:f}=l.pagination;g.value.page=e,g.value.rowsPerPage=a,g.value.sortBy=b,g.value.descending=f,k(g.value)},Ke=l=>{T.value=l,g.value.page=1,k()},$e=()=>{d.value={from:"",to:""}},ue=l=>l?H.formatDate(l,"YYYY-MM-DD HH:mm"):"",Me=async()=>{A.value=!1,Ye(),await ze(),await ve(),_.value=N.value},me=async l=>{A.value=!0;try{const e=await h.get(`/api/pazaauto/pembelian/${l.noPembelian}`);e.data.success?(n.value={...e.data.data},n.value.details||(n.value.details=[]),n.value.tanggalPembelian&&(n.value.tanggalPembelian=n.value.tanggalPembelian.substring(0,16)),n.value.supplierId&&typeof n.value.supplierId=="object"&&(_e.value=n.value.supplierId.namaSupplier)):(n.value={...l,details:l.details||[]},n.value.tanggalPembelian&&(n.value.tanggalPembelian=n.value.tanggalPembelian.substring(0,16)))}catch(e){console.error("Failed to fetch pembelian details:",e),n.value={...l,details:l.details||[]},n.value.tanggalPembelian&&(n.value.tanggalPembelian=n.value.tanggalPembelian.substring(0,16))}va(()=>{Z.value=JSON.parse(JSON.stringify(n.value)),se.value?.selectRowByItem(l)})},pe=l=>{F.value=l,R.value=!0},Je=async()=>{q.value=!0;try{(await h.delete(`/api/pazaauto/pembelian/${F.value.id}`)).data.success&&(p.notify({type:"positive",message:"Pembelian deleted successfully"}),R.value=!1,F.value=null,await k())}catch(l){p.notify({type:"negative",message:"Failed to delete pembelian",caption:l.response?.data?.message||l.message})}finally{q.value=!1}},D=l=>l?new Intl.NumberFormat("id-ID",{style:"currency",currency:"IDR",minimumFractionDigits:0}).format(l):"Rp 0",He=l=>{switch(l){case"LUNAS":return"green";case"BELUM_LUNAS":return"red";case"DP":return"orange";default:return"grey"}},Ye=()=>{n.value={id:null,noPembelian:"",tanggalPembelian:new Date().toISOString().slice(0,16),jenisPembelian:"SPAREPART",jenisOperasional:"",kategoriOperasional:null,supplierId:null,grandTotal:0,statusPembayaran:"BELUM_LUNAS",jenisPembayaran:"CASH",diskon:0,ppn:0,keterangan:"",details:[]}},ze=async()=>{try{const l=await h.get("/api/pazaauto/pembelian/get-next-number");l.data.success&&(n.value.noPembelian=l.data.data)}catch(l){p.notify({type:"negative",message:"Failed to fetch pembelian number",caption:l.response?.data?.message||l.message})}},ve=async()=>{try{const l=await h.get("/api/pazaauto/supplier");l.data.success&&(N.value=l.data.data||[],_.value=N.value)}catch(l){p.notify({type:"negative",message:"Failed to fetch suppliers",caption:l.response?.data?.message||l.message})}},Ge=(l,e)=>{C.value=l,e(()=>{if(!l)_.value=N.value;else{const a=l.toLowerCase();_.value=N.value.filter(b=>b.namaSupplier.toLowerCase().includes(a))}})},We=l=>{C.value=l},Xe=()=>{u.value.namaSupplier=C.value,Q.value=!0},ge=()=>{Q.value=!1,Ze()},Ze=()=>{u.value={namaSupplier:"",alamat:"",telepon:"",email:"",kontakPerson:"",noHpKontak:"",kota:"",kodePos:"",keterangan:""}},ce=async()=>{X.value=!0;try{const l=await h.post("/api/pazaauto/supplier",u.value);if(l.data.success){p.notify({type:"positive",message:"Supplier created successfully"});const e=l.data.data;N.value.push(e),n.value.supplierId=e.id,ge()}}catch(l){p.notify({type:"negative",message:"Failed to create supplier",caption:l.response?.data?.message||l.message})}finally{X.value=!1}},ea=()=>{j.value=!1,W.value=-1,fe(),U.value=!0},aa=l=>{j.value=!0,W.value=n.value.details.findIndex(e=>e.id===l.id),r.value={...l},U.value=!0},be=()=>{U.value=!1,fe()},fe=()=>{r.value={id:null,pembelianId:null,namaItem:"",kategoriItem:n.value.jenisPembelian,harga:0,kuantiti:1,total:0,keterangan:""}},ye=()=>{const l=parseFloat(r.value.harga)||0,e=parseInt(r.value.kuantiti)||0;r.value.total=l*e},Pe=()=>{if(!r.value.namaItem||!r.value.harga||!r.value.kuantiti){p.notify({type:"negative",message:"Mohon lengkapi field yang wajib diisi"});return}if(r.value.harga<=0||r.value.kuantiti<=0){p.notify({type:"negative",message:"Harga dan kuantiti harus lebih dari 0"});return}const l={...r.value,id:r.value.id,pembelianId:n.value.id,harga:parseFloat(r.value.harga),kuantiti:parseInt(r.value.kuantiti),total:parseFloat(r.value.total)};j.value?n.value.details[W.value]=l:n.value.details.push(l),be(),p.notify({type:"positive",message:j.value?"Item berhasil diupdate":"Item berhasil ditambahkan"})},la=async l=>{p.dialog({title:"Konfirmasi Hapus",message:`Apakah Anda yakin ingin menghapus item "${l.namaItem}"?`,cancel:!0,persistent:!0}).onOk(async()=>{try{l.id&&await h.delete(`/api/pazaauto/pembelian-detail/${l.id}`);const e=n.value.details.findIndex(a=>a.id===l.id);e>-1&&(n.value.details.splice(e,1),p.notify({type:"positive",message:"Item berhasil dihapus"}))}catch(e){p.notify({type:"negative",message:"Failed to delete item",caption:e.response?.data?.message||e.message})}})},ta=async()=>{try{const l=await h.get("/api/pazaauto/pembelian/paginated",{params:{page:1,rowsPerPage:1e4,search:T.value,jenisPembelian:S.value,kategoriOperasional:V.value,statusFilter:w.value?w.value.join(","):"",startDate:d.value?.from?d.value.from.replace(/\//g,"-"):"",endDate:d.value?.to?d.value.to.replace(/\//g,"-"):""}});if(l.data.success){const e=l.data.data.rows||l.data.data||[],a={noPenjualan:"LAPORAN PEMBELIAN",namaPelanggan:"LAPORAN PEMBELIAN",tanggal:new Date().toLocaleDateString("id-ID"),grandTotal:e.reduce((f,ra)=>f+(ra.grandTotal||0),0),filters:{search:T.value||"-",dateRange:ne.value||"-",jenisPembelian:S.value||"-",kategoriOperasional:V.value||"-",status:w.value?.join(", ")||"-"},items:e.map(f=>({nama:f.noPembelian,tanggal:ue(f.tanggalPembelian),type:f.jenisPembelian,kategori:f.kategoriOperasional,supplier:f.supplierId?.namaSupplier||f.namaSupplier||"-",harga:f.grandTotal,status:f.statusPembayaran,metode:f.jenisPembayaran}))},b=oa(ka,{data:a,formatCurrency:D,formatNumber:na});G.value=b,B.value=!0}}catch(l){p.notify({type:"negative",message:"Failed to fetch pembelian data for printing",caption:l.response?.data?.message||l.message})}},na=l=>l?new Intl.NumberFormat("id-ID",{minimumFractionDigits:0}).format(l):"0",ia=()=>{let l=document.getElementById("print-iframe");l&&document.body.removeChild(l),l=document.createElement("iframe"),l.id="print-iframe",l.style.position="absolute",l.style.width="0px",l.style.height="0px",l.style.border="none",document.body.appendChild(l);const e=l.contentWindow.document;e.open(),e.write(G.value),e.close(),setTimeout(()=>{l.contentWindow.focus(),l.contentWindow.print()},250)},oa=(l,e)=>{const a=Object.keys(e),b=Object.values(e);try{return new Function(...a,`return \`${l}\`;`)(...b)}catch(f){return console.error("Template rendering error:",f),"Error rendering template"}},sa=async()=>{z.value=!0;try{let l;A.value?l=await h.put(`/api/pazaauto/pembelian/${n.value.id}`,n.value):l=await h.post("/api/pazaauto/pembelian",n.value),l.data.success&&(p.notify({type:"positive",message:A.value?"Pembelian updated successfully":"Pembelian created successfully"}),await k(),A.value?Z.value=JSON.parse(JSON.stringify(n.value)):await me(l.data.data))}catch(l){p.notify({type:"negative",message:"Failed to save pembelian",caption:l.response?.data?.message||l.message})}finally{z.value=!1}};let ee=null;return $(T,()=>{ee&&clearTimeout(ee),ee=setTimeout(()=>{g.value.page=1,k()},500)}),$([S,V],()=>{g.value.page=1,k()},{deep:!0}),$(w,l=>{Ee(l),g.value.page=1,k()},{deep:!0}),$(d,()=>{g.value.page=1,k()},{deep:!0}),ua(()=>{k(),ve()}),(l,e)=>(x(),E(Pa,{padding:""},{default:o(()=>[t(ya,{modelValue:oe.value,"onUpdate:modelValue":e[15]||(e[15]=a=>oe.value=a),limits:[50,100],style:{height:"calc(100vh - 100px)"}},{before:o(()=>[t(fa,{rows:ie.value,columns:Fe,loading:Y.value,pagination:g.value,"onUpdate:pagination":e[4]||(e[4]=a=>g.value=a),onRequest:Qe,onSearch:Ke,"on-create":Me,"on-edit":me,"create-label":"Tambah Pembelian","row-key":"noPembelian",ref_key:"tableRef",ref:se,"search-placeholder":"Search by No Pembelian...",dense:"",footerButtonLabel:"Print",footerButtonAction:ta},Se({"toolbar-filters":o(()=>[i("div",xa,[t(I,{modelValue:S.value,"onUpdate:modelValue":e[0]||(e[0]=a=>S.value=a),options:Ce,label:"Jenis Pembelian",dense:"","options-dense":"",flat:"",outlined:"",clearable:"",style:{"min-width":"150px"}},null,8,["modelValue"]),t(I,{modelValue:V.value,"onUpdate:modelValue":e[1]||(e[1]=a=>V.value=a),options:re,label:"Kategori Operasional",dense:"","options-dense":"",flat:"",outlined:"",clearable:"",style:{"min-width":"150px"}},null,8,["modelValue"]),t(I,{modelValue:w.value,"onUpdate:modelValue":e[2]||(e[2]=a=>w.value=a),multiple:"",options:de.value,label:"Status Pembayaran",dense:"","options-dense":"",flat:"",outlined:"",style:{"min-width":"150px"}},null,8,["modelValue","options"]),t(m,{"model-value":ne.value,label:"Date Range",outlined:"",dense:"",readonly:""},{append:o(()=>[t(xe,{name:"event",class:"cursor-pointer"},{default:o(()=>[t(ga,{cover:"","transition-show":"scale","transition-hide":"scale"},{default:o(()=>[t(ca,{modelValue:d.value,"onUpdate:modelValue":e[3]||(e[3]=a=>d.value=a),range:""},{default:o(()=>[i("div",Sa,[t(c,{label:"Clear",color:"primary",flat:"",onClick:$e}),ma(t(c,{label:"OK",color:"primary",flat:""},null,512),[[ha]])])]),_:1},8,["modelValue"])]),_:1})]),_:1})]),_:1},8,["model-value"])])]),"body-cell-grandTotal":o(a=>[y(P(D(a.row.grandTotal)),1)]),"body-cell-tanggalPembelian":o(a=>[y(P(ue(a.row.tanggalPembelian)),1)]),"body-cell-statusPembayaran":o(a=>[t(ba,{color:He(a.row.statusPembayaran),style:{width:"100px"}},{default:o(()=>[y(P(a.row.statusPembayaran),1)]),_:2},1032,["color"])]),_:2},[v.value?{name:"body-cell-actions",fn:o(a=>[t(c,{flat:"",dense:"",round:"",icon:"delete",color:"negative",onClick:pa(b=>pe(a.row),["stop"])},{default:o(()=>[t(L,null,{default:o(()=>[...e[36]||(e[36]=[y("Delete",-1)])]),_:1})]),_:1},8,["onClick"])]),key:"0"}:void 0]),1032,["rows","loading","pagination"])]),after:o(()=>[i("div",Va,[e[49]||(e[49]=i("div",{class:"row items-center q-mb-lg"},[i("div",{class:"text-h6"},"Detail Pengeluaran")],-1)),t(le,{class:"q-gutter-md",onSubmit:sa},{default:o(()=>[i("div",Aa,[e[37]||(e[37]=i("div",{class:"text-subtitle text-weight-bold text-grey-8 q-mb-sm"},"Informasi Pembelian",-1)),i("div",Ia,[i("div",Oa,[t(m,{modelValue:n.value.noPembelian,"onUpdate:modelValue":e[5]||(e[5]=a=>n.value.noPembelian=a),label:"No Pembelian",outlined:"",dense:"",readonly:""},null,8,["modelValue"])]),i("div",Ta,[t(m,{modelValue:n.value.tanggalPembelian,"onUpdate:modelValue":e[6]||(e[6]=a=>n.value.tanggalPembelian=a),label:"Tanggal Pembelian",outlined:"",dense:"",type:"datetime-local","stack-label":"",readonly:!v.value,rules:[a=>!!a||"Tanggal pembelian harus diisi"],"hide-bottom-space":""},null,8,["modelValue","readonly","rules"])]),i("div",Na,[t(m,{modelValue:n.value.jenisPembelian,"onUpdate:modelValue":e[7]||(e[7]=a=>n.value.jenisPembelian=a),options:Ue,label:"Jenis Pembelian",outlined:"",dense:"",disable:!v.value,"option-label":"label","option-value":"value","emit-value":"","map-options":"",rules:[a=>!!a||"Jenis pembelian harus diisi"],"hide-bottom-space":"",readonly:""},null,8,["modelValue","disable","rules"])])])]),n.value.jenisPembelian==="OPERASIONAL"?(x(),ke("div",Da,[e[38]||(e[38]=i("div",{class:"text-subtitle text-weight-bold text-grey-8 q-mb-sm"},"Informasi Operasional",-1)),i("div",Ea,[i("div",Ra,[t(m,{modelValue:n.value.jenisOperasional,"onUpdate:modelValue":e[8]||(e[8]=a=>n.value.jenisOperasional=a),label:"Jenis Operasional",outlined:"",dense:"",readonly:!v.value,rules:[a=>n.value.jenisPembelian==="OPERASIONAL"?!!a||"Jenis operasional harus diisi":!0]},null,8,["modelValue","readonly","rules"])]),i("div",_a,[t(m,{modelValue:n.value.kategoriOperasional,"onUpdate:modelValue":e[9]||(e[9]=a=>n.value.kategoriOperasional=a),options:re,label:"Kategori Operasional",outlined:"",dense:"",readonly:!v.value,rules:[a=>n.value.jenisPembelian==="OPERASIONAL"?!!a||"Kategori operasional harus diisi":!0]},null,8,["modelValue","readonly","rules"])])])])):O("",!0),n.value.jenisPembelian==="SPAREPART"?(x(),ke("div",Ca,[e[40]||(e[40]=i("div",{class:"text-subtitle text-weight-bold text-grey-8 q-mb-sm"},"Informasi Supplier",-1)),i("div",Ua,[i("div",ja,[t(I,{modelValue:n.value.supplierId,"onUpdate:modelValue":e[10]||(e[10]=a=>n.value.supplierId=a),options:_.value,label:"Supplier",outlined:"",dense:"",disable:!v.value,"option-label":"namaSupplier","option-value":"id","emit-value":"","map-options":"","use-input":"","input-debounce":"300",onFilter:Ge,onInputValue:We,rules:[a=>n.value.jenisPembelian==="SPAREPART"?!!a||"Supplier harus diisi":!0],"hide-bottom-space":"",readonly:""},{"no-option":o(()=>[t(Oe,null,{default:o(()=>[t(Te,{class:"text-grey-6"},{default:o(()=>[...e[39]||(e[39]=[y(" No results ",-1)])]),_:1})]),_:1}),C.value?(x(),E(Oe,{key:0,clickable:"",onClick:Xe},{default:o(()=>[t(Te,null,{default:o(()=>[i("div",La,[t(xe,{name:"add",class:"q-mr-sm"}),y(' Tambah supplier baru: "'+P(C.value)+'" ',1)])]),_:1})]),_:1})):O("",!0)]),_:1},8,["modelValue","options","disable","rules"])])])])):O("",!0),i("div",qa,[i("div",Ba,[i("div",Fa,[e[42]||(e[42]=i("div",null,"Detail pembelian",-1)),v.value?(x(),E(c,{key:0,color:"primary",icon:"add",dense:"",onClick:ea,disable:!v.value},{default:o(()=>[t(L,null,{default:o(()=>[...e[41]||(e[41]=[y("Tambah item pembelian",-1)])]),_:1})]),_:1},8,["disable"])):O("",!0)])]),t(da,{rows:n.value.details,columns:Le,"row-key":"id",flat:"",bordered:"",dense:"","rows-per-page-options":[0],"hide-pagination":""},Se({"body-cell-harga":o(a=>[t(ae,{props:a},{default:o(()=>[y(P(D(a.row.harga)),1)]),_:2},1032,["props"])]),"body-cell-total":o(a=>[t(ae,{props:a},{default:o(()=>[y(P(D(a.row.total)),1)]),_:2},1032,["props"])]),"no-data":o(()=>[e[45]||(e[45]=i("div",{class:"full-width row flex-center text-grey-6 q-pa-md"},[i("div",null,"Belum ada item pembelian")],-1))]),_:2},[v.value?{name:"body-cell-actions",fn:o(a=>[t(ae,{props:a,class:"text-center"},{default:o(()=>[t(c,{flat:"",dense:"",round:"",icon:"edit",color:"primary",onClick:b=>aa(a.row),disable:!v.value},{default:o(()=>[t(L,null,{default:o(()=>[...e[43]||(e[43]=[y("Edit",-1)])]),_:1})]),_:1},8,["onClick","disable"]),t(c,{flat:"",dense:"",round:"",icon:"delete",color:"negative",onClick:b=>la(a.row),disable:!v.value},{default:o(()=>[t(L,null,{default:o(()=>[...e[44]||(e[44]=[y("Delete",-1)])]),_:1})]),_:1},8,["onClick","disable"])]),_:2},1032,["props"])]),key:"0"}:void 0]),1032,["rows"])]),i("div",Qa,[e[46]||(e[46]=i("div",{class:"text-subtitle1 text-weight-bold text-grey-8 q-mb-sm"},"Payment Details",-1)),i("div",Ka,[i("div",$a,[t(he,{label:"Total",outlined:"",dense:"","stack-label":""},{control:o(()=>[i("div",Ma,P(D(qe.value)),1)]),_:1})]),i("div",Ja,[t(I,{modelValue:n.value.statusPembayaran,"onUpdate:modelValue":e[11]||(e[11]=a=>n.value.statusPembayaran=a),options:de.value,label:"Status Pembayaran",outlined:"",dense:"",disable:!v.value,rules:[a=>!!a||"Status pembayaran harus diisi"]},null,8,["modelValue","options","disable","rules"])]),i("div",Ha,[t(I,{modelValue:n.value.jenisPembayaran,"onUpdate:modelValue":e[12]||(e[12]=a=>n.value.jenisPembayaran=a),options:["CASH","TRANSFER","DEBIT","KREDIT"],label:"Metode Pembayaran",outlined:"",dense:"",disable:!v.value,rules:[a=>!!a||"Metode pembayaran harus diisi"]},null,8,["modelValue","disable","rules"])])])]),i("div",null,[e[47]||(e[47]=i("div",{class:"text-subtitle1 text-weight-bold text-grey-8 q-mb-sm"},"Informasi Tambahan",-1)),t(m,{modelValue:n.value.keterangan,"onUpdate:modelValue":e[13]||(e[13]=a=>n.value.keterangan=a),label:"Keterangan",outlined:"",dense:"",type:"textarea",rows:"3",readonly:!v.value},null,8,["modelValue","readonly"])]),i("div",Ya,[A.value&&v.value?(x(),E(c,{key:0,label:"Hapus",color:"negative",flat:"",onClick:e[14]||(e[14]=a=>pe(n.value)),loading:q.value},null,8,["loading"])):O("",!0),v.value?(x(),E(c,{key:1,label:"Simpan",type:"submit",color:"primary",loading:z.value},{default:o(()=>[Be.value?(x(),E(L,{key:0},{default:o(()=>[...e[48]||(e[48]=[y(" Ada perubahan yang belum disimpan ",-1)])]),_:1})):O("",!0)]),_:1},8,["loading"])):O("",!0)])]),_:1})])]),_:1},8,["modelValue"]),t(we,{modelValue:R.value,"onUpdate:modelValue":e[17]||(e[17]=a=>R.value=a),title:"Konfirmasi hapus data","min-width":"400px",position:"standard"},{actions:o(()=>[t(c,{flat:"",label:"Batalkan",color:"primary",onClick:e[16]||(e[16]=a=>R.value=!1)}),t(c,{flat:"",label:"Hapus saja",color:"negative",onClick:Je,loading:q.value},null,8,["loading"])]),default:o(()=>[e[50]||(e[50]=y(" Apakah Anda yakin ingin menghapus data Pembelian ",-1)),i("strong",null,P(F.value?.noPembelian)+" ?",1),e[51]||(e[51]=y("? ",-1))]),_:1},8,["modelValue"]),t(Ve,{modelValue:U.value,"onUpdate:modelValue":e[23]||(e[23]=a=>U.value=a),persistent:""},{default:o(()=>[t(Ae,{style:{"min-width":"500px"}},{default:o(()=>[t(M,null,{default:o(()=>[i("div",za,P(j.value?"Edit Item":"Tambah Item"),1)]),_:1}),t(J),t(M,{class:"q-pt-none"},{default:o(()=>[t(le,{onSubmit:Pe,class:"q-gutter-md"},{default:o(()=>[i("div",Ga,[i("div",Wa,[t(m,{modelValue:r.value.namaItem,"onUpdate:modelValue":e[18]||(e[18]=a=>r.value.namaItem=a),label:"Nama Item*",outlined:"",dense:"",rules:[a=>!!a||"Nama item harus diisi"],"hide-bottom-space":""},null,8,["modelValue","rules"])]),i("div",Xa,[t(I,{modelValue:r.value.kategoriItem,"onUpdate:modelValue":e[19]||(e[19]=a=>r.value.kategoriItem=a),options:je,label:"Kategori Item",outlined:"",dense:"","option-label":"label","option-value":"value","emit-value":"","map-options":"",disabled:"",readonly:""},null,8,["modelValue"])]),i("div",Za,[t(m,{modelValue:r.value.harga,"onUpdate:modelValue":[e[20]||(e[20]=a=>r.value.harga=a),ye],label:"Harga*",outlined:"",dense:"",type:"number",step:"0.01",min:"0",rules:[a=>!!a&&a>0||"Harga harus diisi dan lebih dari 0"],"hide-bottom-space":""},null,8,["modelValue","rules"])]),i("div",el,[t(m,{modelValue:r.value.kuantiti,"onUpdate:modelValue":[e[21]||(e[21]=a=>r.value.kuantiti=a),ye],label:"Kuantiti*",outlined:"",dense:"",type:"number",min:"1",rules:[a=>!!a&&a>0||"Kuantiti harus diisi dan lebih dari 0"],"hide-bottom-space":""},null,8,["modelValue","rules"])]),i("div",al,[t(he,{label:"Total",outlined:"",dense:"","stack-label":""},{control:o(()=>[i("div",ll,P(D(r.value.total)),1)]),_:1})]),i("div",tl,[t(m,{modelValue:r.value.keterangan,"onUpdate:modelValue":e[22]||(e[22]=a=>r.value.keterangan=a),label:"Keterangan",outlined:"",dense:"",type:"textarea",rows:"2"},null,8,["modelValue"])])])]),_:1})]),_:1}),t(J),t(Ie,{align:"right"},{default:o(()=>[t(c,{flat:"",label:"Batal",color:"primary",onClick:be}),t(c,{flat:"",label:"Simpan",color:"primary",onClick:Pe})]),_:1})]),_:1})]),_:1},8,["modelValue"]),t(Ve,{modelValue:Q.value,"onUpdate:modelValue":e[33]||(e[33]=a=>Q.value=a),persistent:""},{default:o(()=>[t(Ae,{style:{"min-width":"500px"}},{default:o(()=>[t(M,null,{default:o(()=>[...e[52]||(e[52]=[i("div",{class:"text-h6"},"Add New Supplier",-1)])]),_:1}),t(J),t(M,{class:"q-pt-none"},{default:o(()=>[t(le,{onSubmit:ce,class:"q-gutter-md"},{default:o(()=>[i("div",nl,[i("div",il,[t(m,{modelValue:u.value.namaSupplier,"onUpdate:modelValue":e[24]||(e[24]=a=>u.value.namaSupplier=a),label:"Supplier Name*",outlined:"",dense:"",rules:[a=>!!a||"Supplier name is required"],"hide-bottom-space":""},null,8,["modelValue","rules"])]),i("div",ol,[t(m,{modelValue:u.value.alamat,"onUpdate:modelValue":e[25]||(e[25]=a=>u.value.alamat=a),label:"Address",outlined:"",dense:"",type:"textarea",rows:"2"},null,8,["modelValue"])]),i("div",sl,[t(m,{modelValue:u.value.telepon,"onUpdate:modelValue":e[26]||(e[26]=a=>u.value.telepon=a),label:"Phone",outlined:"",dense:""},null,8,["modelValue"])]),i("div",rl,[t(m,{modelValue:u.value.email,"onUpdate:modelValue":e[27]||(e[27]=a=>u.value.email=a),label:"Email",outlined:"",dense:"",type:"email"},null,8,["modelValue"])]),i("div",dl,[t(m,{modelValue:u.value.kontakPerson,"onUpdate:modelValue":e[28]||(e[28]=a=>u.value.kontakPerson=a),label:"Contact Person",outlined:"",dense:""},null,8,["modelValue"])]),i("div",ul,[t(m,{modelValue:u.value.noHpKontak,"onUpdate:modelValue":e[29]||(e[29]=a=>u.value.noHpKontak=a),label:"Contact Phone",outlined:"",dense:""},null,8,["modelValue"])]),i("div",ml,[t(m,{modelValue:u.value.kota,"onUpdate:modelValue":e[30]||(e[30]=a=>u.value.kota=a),label:"City",outlined:"",dense:""},null,8,["modelValue"])]),i("div",pl,[t(m,{modelValue:u.value.kodePos,"onUpdate:modelValue":e[31]||(e[31]=a=>u.value.kodePos=a),label:"Postal Code",outlined:"",dense:""},null,8,["modelValue"])]),i("div",vl,[t(m,{modelValue:u.value.keterangan,"onUpdate:modelValue":e[32]||(e[32]=a=>u.value.keterangan=a),label:"Notes",outlined:"",dense:"",type:"textarea",rows:"2"},null,8,["modelValue"])])])]),_:1})]),_:1}),t(J),t(Ie,{align:"right"},{default:o(()=>[t(c,{flat:"",label:"Cancel",color:"primary",onClick:ge}),t(c,{flat:"",label:"Save",color:"primary",onClick:ce,loading:X.value},null,8,["loading"])]),_:1})]),_:1})]),_:1},8,["modelValue"]),t(we,{modelValue:B.value,"onUpdate:modelValue":e[35]||(e[35]=a=>B.value=a),title:"Print Preview","min-width":"800px","max-width":"90vw"},{actions:o(()=>[t(c,{flat:"",label:"Tutup",color:"primary",onClick:e[34]||(e[34]=a=>B.value=!1)}),t(c,{label:"Print",icon:"print",color:"secondary",onClick:ia})]),default:o(()=>[i("div",gl,[i("iframe",{srcdoc:G.value,style:{width:"100%",height:"100%",border:"1px solid #ccc"}},null,8,cl)])]),_:1},8,["modelValue"])]),_:1}))}};export{Cl as default};

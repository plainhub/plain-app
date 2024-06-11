import{d as le,I as re,h as A,g as ie,j as U,ab as w,cg as N,ch as ce,ar as pe,k as _e,as as E,o as d,c as u,a as e,t as a,m as s,x as g,cj as F,O as h,P as b,y as Z,z as x,p as j,q as ee,e as q,al as ve,l as me,C as fe,ck as ge,cl as $e,Y as S,S as he,am as te,V as ae,U as oe,w as ne,ac as ye,a2 as be}from"./index-BxNI00MG.js";import{T as v,a as k,_ as ke,A as we}from"./question-mark-rounded-A5MXITGK.js";import{u as Ce,a as Te}from"./vee-validate.esm-9czZ1sUw.js";const Ne={slot:"headline"},Ie={slot:"content"},Ee={class:"row mb-3"},Fe={class:"col-md-3 col-form-label"},Ae={class:"col-md-9"},Ve=["value"],De={key:0,class:"input-group"},Re=["placeholder"],Me={class:"inner"},Oe={class:"help-block"},qe={value:""},Se=["value"],Ue={key:2,class:"invalid-feedback"},je={class:"row mb-3"},Je={class:"col-md-3 col-form-label"},Le={class:"col-md-9"},Be=["value"],Pe={class:"row mb-3"},ze={class:"col-md-3 col-form-label"},Qe={class:"col-md-9"},Ye={value:"all"},Ge=["value"],He=["value"],Ke={class:"row mb-3"},We={class:"col-md-3 col-form-label"},Xe={class:"col-md-9"},Ze={slot:"actions"},xe=["disabled"],se=le({__name:"EditRouteModal",props:{data:{type:Object},devices:{type:Array},networks:{type:Array}},setup(y){var J,L,B,P,z,Q,Y,G,H;const{handleSubmit:I}=Ce(),r=re({if_name:"",apply_to:"all",notes:"",target:"",is_enabled:!0}),c=A(v.INTERNET),V=Object.values(v).filter(n=>[v.IP,v.NET,v.REMOTE_PORT,v.INTERNET].includes(n)),{t:C}=ie(),m=y,{mutate:D,loading:R,onDone:M}=U({document:w`
    mutation createConfig($input: ConfigInput!) {
      createConfig(input: $input) {
        ...ConfigFragment
      }
    }
    ${N}
  `,options:{update:(n,i)=>{ce(n,i.data.createConfig,w`
          query {
            configs {
              ...ConfigFragment
            }
          }
          ${N}
        `)}}}),{mutate:O,loading:o,onDone:T}=U({document:w`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${N}
  `}),{value:p,resetField:f,errorMessage:l}=Te("inputValue",pe().test("required",n=>"valid.required",n=>!k.hasInput(c.value)||!!n).test("target-value",n=>"invalid_value",n=>k.isValid(c.value,n??""))),_=(J=m.data)==null?void 0:J.data;c.value=((B=(L=m.data)==null?void 0:L.target)==null?void 0:B.type)??v.INTERNET,p.value=((z=(P=m.data)==null?void 0:P.target)==null?void 0:z.value)??"",r.apply_to=((Y=(Q=m.data)==null?void 0:Q.applyTo)==null?void 0:Y.toValue())??"all",r.if_name=(_==null?void 0:_.if_name)??((H=(G=m.networks)==null?void 0:G[0])==null?void 0:H.ifName)??"",r.notes=(_==null?void 0:_.notes)??"",r.is_enabled=(_==null?void 0:_.is_enabled)??!0,_||f(),_e(c,(n,i)=>{(n===v.INTERFACE||i===v.INTERFACE)&&(p.value="")});const $=I(()=>{const n=new k;n.type=c.value,n.value=p.value??"",r.target=n.toValue(),m.data?O({id:m.data.id,input:{group:"route",value:JSON.stringify(r)}}):D({input:{group:"route",value:JSON.stringify(r)}})});return M(()=>{E()}),T(()=>{E()}),(n,i)=>{var K,W,X;const de=ke,ue=ve;return d(),u("md-dialog",null,[e("div",Ne,a(s(_)?n.$t("edit"):n.$t("create")),1),e("div",Ie,[e("div",Ee,[e("label",Fe,a(n.$t("traffic_to")),1),e("div",Ae,[g(e("select",{class:"form-select","onUpdate:modelValue":i[0]||(i[0]=t=>c.value=t)},[(d(!0),u(h,null,b(s(V),t=>(d(),u("option",{key:t,value:t},a(n.$t(`target_type.${t}`)),9,Ve))),128))],512),[[F,c.value]]),s(k).hasInput(c.value)?(d(),u("div",De,[g(e("input",{type:"text",class:"form-control","onUpdate:modelValue":i[1]||(i[1]=t=>x(p)?p.value=t:null),placeholder:n.$t("for_example")+" "+s(k).hint(c.value)},null,8,Re),[[Z,s(p)]]),j(ue,{class:"input-group-text"},{content:ee(()=>[e("pre",Oe,a(n.$t(`examples_${c.value}`)),1)]),default:ee(()=>[e("span",Me,[j(de)])]),_:1})])):q("",!0),c.value===s(v).INTERFACE?g((d(),u("select",{key:1,class:"form-select","onUpdate:modelValue":i[2]||(i[2]=t=>x(p)?p.value=t:null)},[e("option",qe,a(n.$t("all_local_networks")),1),(d(!0),u(h,null,b((K=y.networks)==null?void 0:K.filter(t=>t.type!=="wan"),t=>(d(),u("option",{value:t.ifName},a(t.name),9,Se))),256))],512)),[[F,s(p)]]):q("",!0),s(l)?(d(),u("div",Ue,a(s(l)?n.$t(s(l)):""),1)):q("",!0)])]),e("div",je,[e("label",Je,a(s(C)("route_via")),1),e("div",Le,[g(e("select",{class:"form-select","onUpdate:modelValue":i[3]||(i[3]=t=>r.if_name=t)},[(d(!0),u(h,null,b((W=y.networks)==null?void 0:W.filter(t=>["wan","vpn"].includes(t.type)),t=>(d(),u("option",{key:t.ifName,value:t.ifName},a(t.name),9,Be))),128))],512),[[F,r.if_name]])])]),e("div",Pe,[e("label",ze,a(s(C)("apply_to")),1),e("div",Qe,[g(e("select",{class:"form-select","onUpdate:modelValue":i[4]||(i[4]=t=>r.apply_to=t)},[e("option",Ye,a(n.$t("all_devices")),1),(d(!0),u(h,null,b((X=y.networks)==null?void 0:X.filter(t=>!["wan","vpn"].includes(t.type)),t=>(d(),u("option",{key:t.ifName,value:"iface:"+t.ifName},a(t.name),9,Ge))),128)),(d(!0),u(h,null,b(y.devices,t=>(d(),u("option",{value:"mac:"+t.mac},a(t.name),9,He))),256))],512),[[F,r.apply_to]])])]),e("div",Ke,[e("label",We,a(s(C)("notes")),1),e("div",Xe,[g(e("textarea",{class:"form-control","onUpdate:modelValue":i[5]||(i[5]=t=>r.notes=t),rows:"3"},null,512),[[Z,r.notes]])])])]),e("div",Ze,[e("md-outlined-button",{value:"cancel",onClick:i[6]||(i[6]=(...t)=>s(E)&&s(E)(...t))},a(n.$t("cancel")),1),e("md-filled-button",{value:"save",disabled:s(R)||s(o),onClick:i[7]||(i[7]=(...t)=>s($)&&s($)(...t)),autofocus:""},a(n.$t("save")),9,xe)])])}}}),et={class:"top-app-bar"},tt={class:"title"},at={class:"actions"},ot={class:"table-responsive"},nt={class:"table"},st=e("th",null,"ID",-1),lt={class:"actions two"},it={class:"form-check"},dt=["disabled","onChange","checked"],ut={class:"nowrap"},rt={class:"nowrap"},ct={class:"actions two"},pt=["onClick"],_t=["onClick"],gt=le({__name:"RoutesView",setup(y){const I=A([]),r=A([]),c=A([]),{t:V}=ie();me({handle:(o,T)=>{T?fe(V(T),"error"):(I.value=o.configs.filter(p=>p.group==="route").map(p=>{const f=JSON.parse(p.value),l=new we;l.parse(f.apply_to);const _=new k;return _.parse(f.target),{id:p.id,createdAt:p.createdAt,updatedAt:p.updatedAt,data:f,applyTo:l,target:_}}),r.value=[...o.devices],c.value=[...o.networks])},document:w`
    query {
      configs {
        ...ConfigFragment
      }
      devices {
        ...DeviceFragment
      }
      networks {
        ...NetworkFragment
      }
    }
    ${ge}
    ${N}
    ${$e}
  `});function C(o){S(ye,{id:o.id,name:o.id,gql:w`
      mutation DeleteConfig($id: ID!) {
        deleteConfig(id: $id)
      }
    `,appApi:!1,typeName:"Config"})}function m(o){S(se,{data:o,devices:r,networks:c})}function D(){S(se,{data:null,devices:r,networks:c})}const{mutate:R,loading:M}=U({document:w`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${N}
  `});function O(o){R({id:o.id,input:{group:"route",value:JSON.stringify(o.data)}})}return(o,T)=>{const p=be,f=he("tooltip");return d(),u(h,null,[e("div",et,[e("div",tt,a(o.$t("page_title.routes")),1),e("div",at,[e("button",{type:"button",class:"btn",onClick:D},a(o.$t("create")),1)])]),e("div",ot,[e("table",nt,[e("thead",null,[e("tr",null,[st,e("th",null,a(o.$t("apply_to")),1),e("th",null,a(o.$t("description")),1),e("th",null,a(o.$t("notes")),1),e("th",null,a(o.$t("enabled")),1),e("th",null,a(o.$t("created_at")),1),e("th",null,a(o.$t("updated_at")),1),e("th",lt,a(o.$t("actions")),1)])]),e("tbody",null,[(d(!0),u(h,null,b(I.value,l=>{var _;return d(),u("tr",{key:l.id},[e("td",null,[j(p,{id:l.id,raw:l.data},null,8,["id","raw"])]),e("td",null,a(l.applyTo.getText(o.$t,r.value,c.value)),1),e("td",null,a(o.$t("route_description",{if_name:((_=c.value.find($=>$.ifName==l.data.if_name))==null?void 0:_.name)??l.data.if_name,target:l.target.getText(o.$t,c.value)})),1),e("td",null,a(l.notes),1),e("td",null,[e("div",it,[e("md-checkbox",{"touch-target":"wrapper",disabled:s(M),onChange:$=>O(l),checked:l.data.is_enabled},null,40,dt)])]),e("td",ut,[g((d(),u("time",null,[ae(a(s(oe)(l.createdAt)),1)])),[[f,s(te)(l.createdAt)]])]),e("td",rt,[g((d(),u("time",null,[ae(a(s(oe)(l.updatedAt)),1)])),[[f,s(te)(l.updatedAt)]])]),e("td",ct,[e("a",{href:"#",class:"v-link",onClick:ne($=>m(l),["prevent"])},a(o.$t("edit")),9,pt),e("a",{href:"#",class:"v-link",onClick:ne($=>C(l),["prevent"])},a(o.$t("delete")),9,_t)])])}),128))])])])],64)}}});export{gt as default};

import{_ as ce}from"./FieldId-4a415062.js";import{_ as pe}from"./Breadcrumb-2d9a8da6.js";import{d as le,A as _e,r as A,u as ie,V as J,a7 as S,bS as N,bT as me,a4 as fe,H as ve,a5 as x,o as c,R as ge,v as I,b as e,f as a,K as $,c as p,F as y,y as k,g as l,a9 as V,O as ee,L as te,e as E,k as U,ab as $e,i as he,t as be,Z as L,bV as ye,bW as ke,N as we,P as ae,Q as oe,w as ne,Y as B}from"./index-e9c32210.js";import{T as f,a as w,_ as Ce,A as Te}from"./question-mark-rounded-ebfa7ee3.js";import{_ as Ne}from"./DeleteConfirm.vuevuetypescriptsetuptruelang-f8a248cf.js";import{_ as Fe}from"./VModal.vuevuetypescriptsetuptruelang-22961625.js";import{u as Ie,a as Ve}from"./vee-validate.esm-1d79c7d3.js";import"./stringToArray-22bf8ae4.js";import"./baseIndexOf-70b929c6.js";const Ae={class:"row mb-3"},Ee={class:"col-md-3 col-form-label"},De={class:"col-md-9"},Re=["value"],Me={key:0,class:"input-group mt-2"},Oe=["placeholder"],qe={class:"inner"},Se={class:"help-block"},Ue={value:""},Le=["value"],Be={key:2,class:"invalid-feedback"},Je={class:"row mb-3"},Pe={class:"col-md-3 col-form-label"},je={class:"col-md-9"},Qe=["value"],He={class:"row mb-3"},Ke={class:"col-md-3 col-form-label"},We={class:"col-md-9"},Ye={value:"all"},Ze=["value"],ze=["value"],Ge={class:"row mb-3"},Xe={class:"col-md-3 col-form-label"},xe={class:"col-md-9"},et=["disabled"],se=le({__name:"EditRouteModal",props:{data:{type:Object},devices:{type:Array},networks:{type:Array}},setup(h){var P,j,Q,H,K,W,Y,Z,z;const m=h,{handleSubmit:b}=Ie(),i=_e({if_name:"",apply_to:"all",notes:"",target:"",is_enabled:!0}),_=A(f.INTERNET),D=Object.values(f).filter(n=>[f.IP,f.NET,f.REMOTE_PORT,f.INTERNET].includes(n)),{t:C}=ie(),{mutate:R,loading:M,onDone:O}=J({document:S`
    mutation createConfig($input: ConfigInput!) {
      createConfig(input: $input) {
        ...ConfigFragment
      }
    }
    ${N}
  `,options:{update:(n,r)=>{me(n,r.data.createConfig,S`
          query {
            configs {
              ...ConfigFragment
            }
          }
          ${N}
        `)}}}),{mutate:q,loading:o,onDone:T}=J({document:S`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${N}
  `}),{value:d,resetField:g,errorMessage:s}=Ve("inputValue",fe().test("required",n=>"valid.required",n=>!w.hasInput(_.value)||!!n).test("target-value",n=>"invalid_value",n=>w.isValid(_.value,n??""))),u=(P=m.data)==null?void 0:P.data;_.value=((Q=(j=m.data)==null?void 0:j.target)==null?void 0:Q.type)??f.INTERNET,d.value=((K=(H=m.data)==null?void 0:H.target)==null?void 0:K.value)??"",i.apply_to=((Y=(W=m.data)==null?void 0:W.applyTo)==null?void 0:Y.toValue())??"all",i.if_name=(u==null?void 0:u.if_name)??((z=(Z=m.networks)==null?void 0:Z[0])==null?void 0:z.ifName)??"",i.notes=(u==null?void 0:u.notes)??"",i.is_enabled=(u==null?void 0:u.is_enabled)??!0,u||g(),ve(_,(n,r)=>{(n===f.INTERFACE||r===f.INTERFACE)&&(d.value="")});const v=b(()=>{const n=new w;n.type=_.value,n.value=d.value??"",i.target=n.toValue(),m.data?q({id:m.data.id,input:{group:"route",value:JSON.stringify(i)}}):R({input:{group:"route",value:JSON.stringify(i)}})});return O(()=>{x()}),T(()=>{x()}),(n,r)=>{const de=Ce,re=$e,ue=Fe;return c(),ge(ue,{title:l(u)?n.$t("edit"):n.$t("create")},{body:I(()=>{var F,G,X;return[e("div",Ae,[e("label",Ee,a(n.$t("traffic_to")),1),e("div",De,[$(e("select",{class:"form-select","onUpdate:modelValue":r[0]||(r[0]=t=>_.value=t)},[(c(!0),p(y,null,k(l(D),t=>(c(),p("option",{value:t},a(n.$t(`target_type.${t}`)),9,Re))),256))],512),[[V,_.value]]),l(w).hasInput(_.value)?(c(),p("div",Me,[$(e("input",{type:"text",class:"form-control","onUpdate:modelValue":r[1]||(r[1]=t=>ee(d)?d.value=t:null),placeholder:n.$t("for_example")+" "+l(w).hint(_.value)},null,8,Oe),[[te,l(d)]]),E(re,{class:"input-group-text"},{content:I(()=>[e("pre",Se,a(n.$t(`examples_${_.value}`)),1)]),default:I(()=>[e("span",qe,[E(de,{class:"bi"})])]),_:1})])):U("",!0),_.value===l(f).INTERFACE?$((c(),p("select",{key:1,class:"form-select mt-2","onUpdate:modelValue":r[2]||(r[2]=t=>ee(d)?d.value=t:null)},[e("option",Ue,a(n.$t("all_local_networks")),1),(c(!0),p(y,null,k((F=h.networks)==null?void 0:F.filter(t=>t.type!=="wan"),t=>(c(),p("option",{value:t.ifName},a(t.name),9,Le))),256))],512)),[[V,l(d)]]):U("",!0),l(s)?(c(),p("div",Be,a(l(s)?n.$t(l(s)):""),1)):U("",!0)])]),e("div",Je,[e("label",Pe,a(l(C)("route_via")),1),e("div",je,[$(e("select",{class:"form-select","onUpdate:modelValue":r[3]||(r[3]=t=>i.if_name=t)},[(c(!0),p(y,null,k((G=h.networks)==null?void 0:G.filter(t=>["wan","vpn"].includes(t.type)),t=>(c(),p("option",{key:t.ifName,value:t.ifName},a(t.name),9,Qe))),128))],512),[[V,i.if_name]])])]),e("div",He,[e("label",Ke,a(l(C)("apply_to")),1),e("div",We,[$(e("select",{class:"form-select","onUpdate:modelValue":r[4]||(r[4]=t=>i.apply_to=t)},[e("option",Ye,a(n.$t("all_devices")),1),(c(!0),p(y,null,k((X=h.networks)==null?void 0:X.filter(t=>!["wan","vpn"].includes(t.type)),t=>(c(),p("option",{key:t.ifName,value:"iface:"+t.ifName},a(t.name),9,Ze))),128)),(c(!0),p(y,null,k(h.devices,t=>(c(),p("option",{value:"mac:"+t.mac},a(t.name),9,ze))),256))],512),[[V,i.apply_to]])])]),e("div",Ge,[e("label",Xe,a(l(C)("notes")),1),e("div",xe,[$(e("textarea",{class:"form-control","onUpdate:modelValue":r[5]||(r[5]=t=>i.notes=t),rows:"3"},null,512),[[te,i.notes]])])])]}),action:I(()=>[e("button",{type:"button",disabled:l(M)||l(o),class:"btn",onClick:r[6]||(r[6]=(...F)=>l(v)&&l(v)(...F))},a(n.$t("save")),9,et)]),_:1},8,["title"])}}}),tt={class:"page-container container"},at={class:"main"},ot={class:"v-toolbar"},nt={class:"table"},st=e("th",null,"ID",-1),lt={class:"actions two"},it={class:"form-check"},dt=["disabled","onChange","onUpdate:modelValue"],rt=["title"],ut=["title"],ct={class:"actions two"},pt=["onClick"],_t=["onClick"],wt=le({__name:"RoutesView",setup(h){const m=A([]),b=A([]),i=A([]),{t:_}=ie();he({handle:(o,T)=>{T?be(_(T),"error"):(m.value=o.configs.filter(d=>d.group==="route").map(d=>{const g=JSON.parse(d.value),s=new Te;s.parse(g.apply_to);const u=new w;return u.parse(g.target),{id:d.id,createdAt:d.createdAt,updatedAt:d.updatedAt,data:g,applyTo:s,target:u}}),b.value=[...o.devices],i.value=[...o.networks])},document:L`
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
    ${ye}
    ${N}
    ${ke}
  `});function D(o){B(Ne,{id:o.id,name:o.id,gql:L`
      mutation DeleteConfig($id: ID!) {
        deleteConfig(id: $id)
      }
    `,appApi:!1,typeName:"Config"})}function C(o){B(se,{data:o,devices:b,networks:i})}function R(){B(se,{data:null,devices:b,networks:i})}const{mutate:M,loading:O}=J({document:L`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${N}
  `});function q(o){M({id:o.id,input:{group:"route",value:JSON.stringify(o.data)}})}return(o,T)=>{const d=pe,g=ce;return c(),p("div",tt,[e("div",at,[e("div",ot,[E(d,{current:()=>o.$t("page_title.routes")},null,8,["current"]),e("button",{type:"button",class:"btn right-actions",onClick:R},a(o.$t("create")),1)]),e("table",nt,[e("thead",null,[e("tr",null,[st,e("th",null,a(o.$t("apply_to")),1),e("th",null,a(o.$t("description")),1),e("th",null,a(o.$t("notes")),1),e("th",null,a(o.$t("enabled")),1),e("th",null,a(o.$t("created_at")),1),e("th",null,a(o.$t("updated_at")),1),e("th",lt,a(o.$t("actions")),1)])]),e("tbody",null,[(c(!0),p(y,null,k(m.value,s=>{var u;return c(),p("tr",{key:s.id},[e("td",null,[E(g,{id:s.id,raw:s.data},null,8,["id","raw"])]),e("td",null,a(s.applyTo.getText(o.$t,b.value,i.value)),1),e("td",null,a(o.$t("route_description",{if_name:((u=i.value.find(v=>v.ifName==s.data.if_name))==null?void 0:u.name)??s.data.if_name,target:s.target.getText(o.$t,i.value)})),1),e("td",null,a(s.notes),1),e("td",null,[e("div",it,[$(e("input",{class:"form-check-input",disabled:l(O),onChange:v=>q(s),"onUpdate:modelValue":v=>s.data.is_enabled=v,type:"checkbox"},null,40,dt),[[we,s.data.is_enabled]])])]),e("td",{class:"nowrap",title:l(ae)(s.createdAt)},a(l(oe)(s.createdAt)),9,rt),e("td",{class:"nowrap",title:l(ae)(s.updatedAt)},a(l(oe)(s.updatedAt)),9,ut),e("td",ct,[e("a",{href:"#",class:"v-link",onClick:ne(v=>C(s),["prevent"])},a(o.$t("edit")),9,pt),e("a",{href:"#",class:"v-link",onClick:ne(v=>D(s),["prevent"])},a(o.$t("delete")),9,_t)])])}),128))])])])])}}});export{wt as default};

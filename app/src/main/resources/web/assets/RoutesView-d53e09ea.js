import{_ as ce}from"./FieldId-e37ba71f.js";import{_ as pe}from"./Breadcrumb-731a6814.js";import{d as le,B as _e,r as A,u as ie,Y as L,ae as O,bT as N,bU as me,ab as fe,J as ve,ac as x,o as c,T as ge,x as I,b as e,f as a,M as $,c as p,F as y,z as k,g as l,ag as E,Q as ee,N as te,e as V,k as S,ai as $e,i as he,t as be,a1 as B,bW as ye,bX as ke,P as we,R as ae,S as oe,w as ne,a0 as J}from"./index-66bea2e9.js";import{T as f,a as w,_ as Ce,A as Te}from"./question-mark-rounded-fdf2fb13.js";import{_ as Ne}from"./DeleteConfirm.vuevuetypescriptsetuptruelang-f7245844.js";import{_ as Fe}from"./VModal.vuevuetypescriptsetuptruelang-091ab08d.js";import{u as Ie,a as Ee}from"./vee-validate.esm-18b68f3d.js";import"./stringToArray-243d75d5.js";const Ae={class:"row mb-3"},Ve={class:"col-md-3 col-form-label"},De={class:"col-md-9"},Me=["value"],Re={key:0,class:"input-group mt-2"},Ue=["placeholder"],qe={class:"inner"},Oe={class:"help-block"},Se={value:""},Be=["value"],Je={key:2,class:"invalid-feedback"},Le={class:"row mb-3"},Pe={class:"col-md-3 col-form-label"},je={class:"col-md-9"},Qe=["value"],ze={class:"row mb-3"},We={class:"col-md-3 col-form-label"},Xe={class:"col-md-9"},Ye={value:"all"},Ge=["value"],He=["value"],Ke={class:"row mb-3"},Ze={class:"col-md-3 col-form-label"},xe={class:"col-md-9"},et=["disabled"],se=le({__name:"EditRouteModal",props:{data:{type:Object},devices:{type:Array},networks:{type:Array}},setup(h){var P,j,Q,z,W,X,Y,G,H;const m=h,{handleSubmit:b}=Ie(),i=_e({if_name:"",apply_to:"all",notes:"",target:"",is_enabled:!0}),_=A(f.INTERNET),D=Object.values(f).filter(n=>[f.IP,f.NET,f.REMOTE_PORT,f.INTERNET].includes(n)),{t:C}=ie(),{mutate:M,loading:R,onDone:U}=L({document:O`
    mutation createConfig($input: ConfigInput!) {
      createConfig(input: $input) {
        ...ConfigFragment
      }
    }
    ${N}
  `,options:{update:(n,r)=>{me(n,r.data.createConfig,O`
          query {
            configs {
              ...ConfigFragment
            }
          }
          ${N}
        `)}}}),{mutate:q,loading:o,onDone:T}=L({document:O`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${N}
  `}),{value:d,resetField:g,errorMessage:s}=Ee("inputValue",fe().test("required",n=>"valid.required",n=>!w.hasInput(_.value)||!!n).test("target-value",n=>"invalid_value",n=>w.isValid(_.value,n??""))),u=(P=m.data)==null?void 0:P.data;_.value=((Q=(j=m.data)==null?void 0:j.target)==null?void 0:Q.type)??f.INTERNET,d.value=((W=(z=m.data)==null?void 0:z.target)==null?void 0:W.value)??"",i.apply_to=((Y=(X=m.data)==null?void 0:X.applyTo)==null?void 0:Y.toValue())??"all",i.if_name=(u==null?void 0:u.if_name)??((H=(G=m.networks)==null?void 0:G[0])==null?void 0:H.ifName)??"",i.notes=(u==null?void 0:u.notes)??"",i.is_enabled=(u==null?void 0:u.is_enabled)??!0,u||g(),ve(_,(n,r)=>{(n===f.INTERFACE||r===f.INTERFACE)&&(d.value="")});const v=b(()=>{const n=new w;n.type=_.value,n.value=d.value??"",i.target=n.toValue(),m.data?q({id:m.data.id,input:{group:"route",value:JSON.stringify(i)}}):M({input:{group:"route",value:JSON.stringify(i)}})});return U(()=>{x()}),T(()=>{x()}),(n,r)=>{const de=Ce,re=$e,ue=Fe;return c(),ge(ue,{title:l(u)?n.$t("edit"):n.$t("create")},{body:I(()=>{var F,K,Z;return[e("div",Ae,[e("label",Ve,a(n.$t("traffic_to")),1),e("div",De,[$(e("select",{class:"form-select","onUpdate:modelValue":r[0]||(r[0]=t=>_.value=t)},[(c(!0),p(y,null,k(l(D),t=>(c(),p("option",{value:t},a(n.$t(`target_type.${t}`)),9,Me))),256))],512),[[E,_.value]]),l(w).hasInput(_.value)?(c(),p("div",Re,[$(e("input",{type:"text",class:"form-control","onUpdate:modelValue":r[1]||(r[1]=t=>ee(d)?d.value=t:null),placeholder:n.$t("for_example")+" "+l(w).hint(_.value)},null,8,Ue),[[te,l(d)]]),V(re,{class:"input-group-text"},{content:I(()=>[e("pre",Oe,a(n.$t(`examples_${_.value}`)),1)]),default:I(()=>[e("span",qe,[V(de,{class:"bi"})])]),_:1})])):S("",!0),_.value===l(f).INTERFACE?$((c(),p("select",{key:1,class:"form-select mt-2","onUpdate:modelValue":r[2]||(r[2]=t=>ee(d)?d.value=t:null)},[e("option",Se,a(n.$t("all_local_networks")),1),(c(!0),p(y,null,k((F=h.networks)==null?void 0:F.filter(t=>t.type!=="wan"),t=>(c(),p("option",{value:t.ifName},a(t.name),9,Be))),256))],512)),[[E,l(d)]]):S("",!0),l(s)?(c(),p("div",Je,a(l(s)?n.$t(l(s)):""),1)):S("",!0)])]),e("div",Le,[e("label",Pe,a(l(C)("route_via")),1),e("div",je,[$(e("select",{class:"form-select","onUpdate:modelValue":r[3]||(r[3]=t=>i.if_name=t)},[(c(!0),p(y,null,k((K=h.networks)==null?void 0:K.filter(t=>["wan","vpn"].includes(t.type)),t=>(c(),p("option",{key:t.ifName,value:t.ifName},a(t.name),9,Qe))),128))],512),[[E,i.if_name]])])]),e("div",ze,[e("label",We,a(l(C)("apply_to")),1),e("div",Xe,[$(e("select",{class:"form-select","onUpdate:modelValue":r[4]||(r[4]=t=>i.apply_to=t)},[e("option",Ye,a(n.$t("all_devices")),1),(c(!0),p(y,null,k((Z=h.networks)==null?void 0:Z.filter(t=>!["wan","vpn"].includes(t.type)),t=>(c(),p("option",{key:t.ifName,value:"iface:"+t.ifName},a(t.name),9,Ge))),128)),(c(!0),p(y,null,k(h.devices,t=>(c(),p("option",{value:"mac:"+t.mac},a(t.name),9,He))),256))],512),[[E,i.apply_to]])])]),e("div",Ke,[e("label",Ze,a(l(C)("notes")),1),e("div",xe,[$(e("textarea",{class:"form-control","onUpdate:modelValue":r[5]||(r[5]=t=>i.notes=t),rows:"3"},null,512),[[te,i.notes]])])])]}),action:I(()=>[e("button",{type:"button",disabled:l(R)||l(o),class:"btn",onClick:r[6]||(r[6]=(...F)=>l(v)&&l(v)(...F))},a(n.$t("save")),9,et)]),_:1},8,["title"])}}}),tt={class:"page-container container"},at={class:"main"},ot={class:"v-toolbar"},nt={class:"table"},st=e("th",null,"ID",-1),lt={class:"actions two"},it={class:"form-check"},dt=["disabled","onChange","onUpdate:modelValue"],rt=["title"],ut=["title"],ct={class:"actions two"},pt=["onClick"],_t=["onClick"],kt=le({__name:"RoutesView",setup(h){const m=A([]),b=A([]),i=A([]),{t:_}=ie();he({handle:(o,T)=>{T?be(_(T),"error"):(m.value=o.configs.filter(d=>d.group==="route").map(d=>{const g=JSON.parse(d.value),s=new Te;s.parse(g.apply_to);const u=new w;return u.parse(g.target),{id:d.id,createdAt:d.createdAt,updatedAt:d.updatedAt,data:g,applyTo:s,target:u}}),b.value=[...o.devices],i.value=[...o.networks])},document:B`
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
  `});function D(o){J(Ne,{id:o.id,name:o.id,gql:B`
      mutation DeleteConfig($id: ID!) {
        deleteConfig(id: $id)
      }
    `,appApi:!1,typeName:"Config"})}function C(o){J(se,{data:o,devices:b,networks:i})}function M(){J(se,{data:null,devices:b,networks:i})}const{mutate:R,loading:U}=L({document:B`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${N}
  `});function q(o){R({id:o.id,input:{group:"route",value:JSON.stringify(o.data)}})}return(o,T)=>{const d=pe,g=ce;return c(),p("div",tt,[e("div",at,[e("div",ot,[V(d,{current:()=>o.$t("page_title.routes")},null,8,["current"]),e("button",{type:"button",class:"btn right-actions",onClick:M},a(o.$t("create")),1)]),e("table",nt,[e("thead",null,[e("tr",null,[st,e("th",null,a(o.$t("apply_to")),1),e("th",null,a(o.$t("description")),1),e("th",null,a(o.$t("notes")),1),e("th",null,a(o.$t("enabled")),1),e("th",null,a(o.$t("created_at")),1),e("th",null,a(o.$t("updated_at")),1),e("th",lt,a(o.$t("actions")),1)])]),e("tbody",null,[(c(!0),p(y,null,k(m.value,s=>{var u;return c(),p("tr",{key:s.id},[e("td",null,[V(g,{id:s.id,raw:s.data},null,8,["id","raw"])]),e("td",null,a(s.applyTo.getText(o.$t,b.value,i.value)),1),e("td",null,a(o.$t("route_description",{if_name:((u=i.value.find(v=>v.ifName==s.data.if_name))==null?void 0:u.name)??s.data.if_name,target:s.target.getText(o.$t,i.value)})),1),e("td",null,a(s.notes),1),e("td",null,[e("div",it,[$(e("input",{class:"form-check-input",disabled:l(U),onChange:v=>q(s),"onUpdate:modelValue":v=>s.data.is_enabled=v,type:"checkbox"},null,40,dt),[[we,s.data.is_enabled]])])]),e("td",{class:"nowrap",title:l(ae)(s.createdAt)},a(l(oe)(s.createdAt)),9,rt),e("td",{class:"nowrap",title:l(ae)(s.updatedAt)},a(l(oe)(s.updatedAt)),9,ut),e("td",ct,[e("a",{href:"#",class:"v-link",onClick:ne(v=>C(s),["prevent"])},a(o.$t("edit")),9,pt),e("a",{href:"#",class:"v-link",onClick:ne(v=>D(s),["prevent"])},a(o.$t("delete")),9,_t)])])}),128))])])])])}}});export{kt as default};

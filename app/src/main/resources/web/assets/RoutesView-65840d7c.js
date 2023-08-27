import{_ as ce}from"./FieldId-4280c3b8.js";import{_ as pe}from"./Breadcrumb-99ff7c19.js";import{d as le,C as _e,r as E,u as ie,Z as J,af as q,bW as N,bX as me,ac as fe,K as ve,ad as x,o as c,U as ge,y as I,e,g as a,N as $,c as p,F as y,A as k,j as l,ah as A,R as ee,O as te,f as V,l as S,aj as $e,i as he,t as be,a2 as j,bZ as ye,b_ as ke,Q as Ce,S as ae,T as oe,w as ne,a1 as B}from"./index-cf22a9d8.js";import{T as f,a as C,_ as we,A as Te}from"./question-mark-rounded-956e83f8.js";import{_ as Ne}from"./DeleteConfirm.vuevuetypescriptsetuptruelang-1943d7a1.js";import{_ as Fe}from"./VModal.vuevuetypescriptsetuptruelang-83ac7574.js";import{u as Ie,a as Ae}from"./vee-validate.esm-dd3b5249.js";import"./stringToArray-2bab501e.js";const Ee={class:"row mb-3"},Ve={class:"col-md-3 col-form-label"},De={class:"col-md-9"},Re=["value"],Me={key:0,class:"input-group mt-2"},Oe=["placeholder"],Ue={class:"inner"},qe={class:"help-block"},Se={value:""},je=["value"],Be={key:2,class:"invalid-feedback"},Je={class:"row mb-3"},Le={class:"col-md-3 col-form-label"},Pe={class:"col-md-9"},Qe=["value"],Ze={class:"row mb-3"},Ke={class:"col-md-3 col-form-label"},We={class:"col-md-9"},Xe={value:"all"},ze=["value"],Ge=["value"],He={class:"row mb-3"},Ye={class:"col-md-3 col-form-label"},xe={class:"col-md-9"},et=["disabled"],se=le({__name:"EditRouteModal",props:{data:{type:Object},devices:{type:Array},networks:{type:Array}},setup(h){var L,P,Q,Z,K,W,X,z,G;const m=h,{handleSubmit:b}=Ie(),i=_e({if_name:"",apply_to:"all",notes:"",target:"",is_enabled:!0}),_=E(f.INTERNET),D=Object.values(f).filter(n=>[f.IP,f.NET,f.REMOTE_PORT,f.INTERNET].includes(n)),{t:w}=ie(),{mutate:R,loading:M,onDone:O}=J({document:q`
    mutation createConfig($input: ConfigInput!) {
      createConfig(input: $input) {
        ...ConfigFragment
      }
    }
    ${N}
  `,options:{update:(n,r)=>{me(n,r.data.createConfig,q`
          query {
            configs {
              ...ConfigFragment
            }
          }
          ${N}
        `)}}}),{mutate:U,loading:o,onDone:T}=J({document:q`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${N}
  `}),{value:d,resetField:g,errorMessage:s}=Ae("inputValue",fe().test("required",n=>"valid.required",n=>!C.hasInput(_.value)||!!n).test("target-value",n=>"invalid_value",n=>C.isValid(_.value,n??""))),u=(L=m.data)==null?void 0:L.data;_.value=((Q=(P=m.data)==null?void 0:P.target)==null?void 0:Q.type)??f.INTERNET,d.value=((K=(Z=m.data)==null?void 0:Z.target)==null?void 0:K.value)??"",i.apply_to=((X=(W=m.data)==null?void 0:W.applyTo)==null?void 0:X.toValue())??"all",i.if_name=(u==null?void 0:u.if_name)??((G=(z=m.networks)==null?void 0:z[0])==null?void 0:G.ifName)??"",i.notes=(u==null?void 0:u.notes)??"",i.is_enabled=(u==null?void 0:u.is_enabled)??!0,u||g(),ve(_,(n,r)=>{(n===f.INTERFACE||r===f.INTERFACE)&&(d.value="")});const v=b(()=>{const n=new C;n.type=_.value,n.value=d.value??"",i.target=n.toValue(),m.data?U({id:m.data.id,input:{group:"route",value:JSON.stringify(i)}}):R({input:{group:"route",value:JSON.stringify(i)}})});return O(()=>{x()}),T(()=>{x()}),(n,r)=>{const de=we,re=$e,ue=Fe;return c(),ge(ue,{title:l(u)?n.$t("edit"):n.$t("create")},{body:I(()=>{var F,H,Y;return[e("div",Ee,[e("label",Ve,a(n.$t("traffic_to")),1),e("div",De,[$(e("select",{class:"form-select","onUpdate:modelValue":r[0]||(r[0]=t=>_.value=t)},[(c(!0),p(y,null,k(l(D),t=>(c(),p("option",{value:t},a(n.$t(`target_type.${t}`)),9,Re))),256))],512),[[A,_.value]]),l(C).hasInput(_.value)?(c(),p("div",Me,[$(e("input",{type:"text",class:"form-control","onUpdate:modelValue":r[1]||(r[1]=t=>ee(d)?d.value=t:null),placeholder:n.$t("for_example")+" "+l(C).hint(_.value)},null,8,Oe),[[te,l(d)]]),V(re,{class:"input-group-text"},{content:I(()=>[e("pre",qe,a(n.$t(`examples_${_.value}`)),1)]),default:I(()=>[e("span",Ue,[V(de,{class:"bi"})])]),_:1})])):S("",!0),_.value===l(f).INTERFACE?$((c(),p("select",{key:1,class:"form-select mt-2","onUpdate:modelValue":r[2]||(r[2]=t=>ee(d)?d.value=t:null)},[e("option",Se,a(n.$t("all_local_networks")),1),(c(!0),p(y,null,k((F=h.networks)==null?void 0:F.filter(t=>t.type!=="wan"),t=>(c(),p("option",{value:t.ifName},a(t.name),9,je))),256))],512)),[[A,l(d)]]):S("",!0),l(s)?(c(),p("div",Be,a(l(s)?n.$t(l(s)):""),1)):S("",!0)])]),e("div",Je,[e("label",Le,a(l(w)("route_via")),1),e("div",Pe,[$(e("select",{class:"form-select","onUpdate:modelValue":r[3]||(r[3]=t=>i.if_name=t)},[(c(!0),p(y,null,k((H=h.networks)==null?void 0:H.filter(t=>["wan","vpn"].includes(t.type)),t=>(c(),p("option",{key:t.ifName,value:t.ifName},a(t.name),9,Qe))),128))],512),[[A,i.if_name]])])]),e("div",Ze,[e("label",Ke,a(l(w)("apply_to")),1),e("div",We,[$(e("select",{class:"form-select","onUpdate:modelValue":r[4]||(r[4]=t=>i.apply_to=t)},[e("option",Xe,a(n.$t("all_devices")),1),(c(!0),p(y,null,k((Y=h.networks)==null?void 0:Y.filter(t=>!["wan","vpn"].includes(t.type)),t=>(c(),p("option",{key:t.ifName,value:"iface:"+t.ifName},a(t.name),9,ze))),128)),(c(!0),p(y,null,k(h.devices,t=>(c(),p("option",{value:"mac:"+t.mac},a(t.name),9,Ge))),256))],512),[[A,i.apply_to]])])]),e("div",He,[e("label",Ye,a(l(w)("notes")),1),e("div",xe,[$(e("textarea",{class:"form-control","onUpdate:modelValue":r[5]||(r[5]=t=>i.notes=t),rows:"3"},null,512),[[te,i.notes]])])])]}),action:I(()=>[e("button",{type:"button",disabled:l(M)||l(o),class:"btn",onClick:r[6]||(r[6]=(...F)=>l(v)&&l(v)(...F))},a(n.$t("save")),9,et)]),_:1},8,["title"])}}}),tt={class:"page-container container"},at={class:"main"},ot={class:"v-toolbar"},nt={class:"table"},st=e("th",null,"ID",-1),lt={class:"actions two"},it={class:"form-check"},dt=["disabled","onChange","onUpdate:modelValue"],rt=["title"],ut=["title"],ct={class:"actions two"},pt=["onClick"],_t=["onClick"],kt=le({__name:"RoutesView",setup(h){const m=E([]),b=E([]),i=E([]),{t:_}=ie();he({handle:(o,T)=>{T?be(_(T),"error"):(m.value=o.configs.filter(d=>d.group==="route").map(d=>{const g=JSON.parse(d.value),s=new Te;s.parse(g.apply_to);const u=new C;return u.parse(g.target),{id:d.id,createdAt:d.createdAt,updatedAt:d.updatedAt,data:g,applyTo:s,target:u}}),b.value=[...o.devices],i.value=[...o.networks])},document:j`
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
  `});function D(o){B(Ne,{id:o.id,name:o.id,gql:j`
      mutation DeleteConfig($id: ID!) {
        deleteConfig(id: $id)
      }
    `,appApi:!1,typeName:"Config"})}function w(o){B(se,{data:o,devices:b,networks:i})}function R(){B(se,{data:null,devices:b,networks:i})}const{mutate:M,loading:O}=J({document:j`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${N}
  `});function U(o){M({id:o.id,input:{group:"route",value:JSON.stringify(o.data)}})}return(o,T)=>{const d=pe,g=ce;return c(),p("div",tt,[e("div",at,[e("div",ot,[V(d,{current:()=>o.$t("page_title.routes")},null,8,["current"]),e("button",{type:"button",class:"btn right-actions",onClick:R},a(o.$t("create")),1)]),e("table",nt,[e("thead",null,[e("tr",null,[st,e("th",null,a(o.$t("apply_to")),1),e("th",null,a(o.$t("description")),1),e("th",null,a(o.$t("notes")),1),e("th",null,a(o.$t("enabled")),1),e("th",null,a(o.$t("created_at")),1),e("th",null,a(o.$t("updated_at")),1),e("th",lt,a(o.$t("actions")),1)])]),e("tbody",null,[(c(!0),p(y,null,k(m.value,s=>{var u;return c(),p("tr",{key:s.id},[e("td",null,[V(g,{id:s.id,raw:s.data},null,8,["id","raw"])]),e("td",null,a(s.applyTo.getText(o.$t,b.value,i.value)),1),e("td",null,a(o.$t("route_description",{if_name:((u=i.value.find(v=>v.ifName==s.data.if_name))==null?void 0:u.name)??s.data.if_name,target:s.target.getText(o.$t,i.value)})),1),e("td",null,a(s.notes),1),e("td",null,[e("div",it,[$(e("input",{class:"form-check-input",disabled:l(O),onChange:v=>U(s),"onUpdate:modelValue":v=>s.data.is_enabled=v,type:"checkbox"},null,40,dt),[[Ce,s.data.is_enabled]])])]),e("td",{class:"nowrap",title:l(ae)(s.createdAt)},a(l(oe)(s.createdAt)),9,rt),e("td",{class:"nowrap",title:l(ae)(s.updatedAt)},a(l(oe)(s.updatedAt)),9,ut),e("td",ct,[e("a",{href:"#",class:"v-link",onClick:ne(v=>w(s),["prevent"])},a(o.$t("edit")),9,pt),e("a",{href:"#",class:"v-link",onClick:ne(v=>D(s),["prevent"])},a(o.$t("delete")),9,_t)])])}),128))])])])])}}});export{kt as default};

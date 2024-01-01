import{d as ie,L as ce,r as A,u as de,i as j,aq as S,b_ as I,b$ as pe,g as _e,w as me,ao as F,o as u,c as r,a as e,t as o,k as l,n as f,c1 as E,F as y,K as k,v as x,p as ee,q as V,I as te,l as U,at as ve,h as fe,y as ge,ac as L,c2 as $e,c3 as he,S as be,a3 as J,Z as ae,j as oe,$ as ne,m as se,ad as ye,a6 as ke}from"./index-2583f876.js";import{_ as we}from"./Breadcrumb-ef080890.js";import{T as m,a as w,_ as Ce,A as Te}from"./question-mark-rounded-1ac12000.js";import{u as Ne,a as Ie}from"./vee-validate.esm-b0601c3b.js";const Fe={slot:"headline"},Ee={slot:"content"},Ae={class:"row mb-3"},Ve={class:"col-md-3 col-form-label"},De={class:"col-md-9"},Re=["value"],qe={key:0,class:"input-group mt-2"},Me=["placeholder"],Oe={class:"inner"},Se={class:"help-block"},Ue={value:""},Le=["value"],Je={key:2,class:"invalid-feedback"},je={class:"row mb-3"},Be={class:"col-md-3 col-form-label"},Pe={class:"col-md-9"},Ke=["value"],Qe={class:"row mb-3"},Ze={class:"col-md-3 col-form-label"},ze={class:"col-md-9"},Ge={value:"all"},He=["value"],We=["value"],Xe={class:"row mb-3"},Ye={class:"col-md-3 col-form-label"},xe={class:"col-md-9"},et={slot:"actions"},tt=["disabled"],le=ie({__name:"EditRouteModal",props:{data:{type:Object},devices:{type:Array},networks:{type:Array}},setup(h){var $,B,P,K,Q,Z,z,G,H;const _=h,{handleSubmit:b}=Ne(),i=ce({if_name:"",apply_to:"all",notes:"",target:"",is_enabled:!0}),p=A(m.INTERNET),D=Object.values(m).filter(s=>[m.IP,m.NET,m.REMOTE_PORT,m.INTERNET].includes(s)),{t:C}=de(),{mutate:R,loading:q,onDone:M}=j({document:S`
    mutation createConfig($input: ConfigInput!) {
      createConfig(input: $input) {
        ...ConfigFragment
      }
    }
    ${I}
  `,options:{update:(s,d)=>{pe(s,d.data.createConfig,S`
          query {
            configs {
              ...ConfigFragment
            }
          }
          ${I}
        `)}}}),{mutate:O,loading:n,onDone:T}=j({document:S`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${I}
  `}),{value:c,resetField:g,errorMessage:v}=Ie("inputValue",_e().test("required",s=>"valid.required",s=>!w.hasInput(p.value)||!!s).test("target-value",s=>"invalid_value",s=>w.isValid(p.value,s??""))),a=($=_.data)==null?void 0:$.data;p.value=((P=(B=_.data)==null?void 0:B.target)==null?void 0:P.type)??m.INTERNET,c.value=((Q=(K=_.data)==null?void 0:K.target)==null?void 0:Q.value)??"",i.apply_to=((z=(Z=_.data)==null?void 0:Z.applyTo)==null?void 0:z.toValue())??"all",i.if_name=(a==null?void 0:a.if_name)??((H=(G=_.networks)==null?void 0:G[0])==null?void 0:H.ifName)??"",i.notes=(a==null?void 0:a.notes)??"",i.is_enabled=(a==null?void 0:a.is_enabled)??!0,a||g(),me(p,(s,d)=>{(s===m.INTERFACE||d===m.INTERFACE)&&(c.value="")});const N=b(()=>{const s=new w;s.type=p.value,s.value=c.value??"",i.target=s.toValue(),_.data?O({id:_.data.id,input:{group:"route",value:JSON.stringify(i)}}):R({input:{group:"route",value:JSON.stringify(i)}})});return M(()=>{F()}),T(()=>{F()}),(s,d)=>{var W,X,Y;const ue=Ce,re=ve;return u(),r("md-dialog",null,[e("div",Fe,o(l(a)?s.$t("edit"):s.$t("create")),1),e("div",Ee,[e("div",Ae,[e("label",Ve,o(s.$t("traffic_to")),1),e("div",De,[f(e("select",{class:"form-select","onUpdate:modelValue":d[0]||(d[0]=t=>p.value=t)},[(u(!0),r(y,null,k(l(D),t=>(u(),r("option",{value:t},o(s.$t(`target_type.${t}`)),9,Re))),256))],512),[[E,p.value]]),l(w).hasInput(p.value)?(u(),r("div",qe,[f(e("input",{type:"text",class:"form-control","onUpdate:modelValue":d[1]||(d[1]=t=>ee(c)?c.value=t:null),placeholder:s.$t("for_example")+" "+l(w).hint(p.value)},null,8,Me),[[x,l(c)]]),V(re,{class:"input-group-text"},{content:te(()=>[e("pre",Se,o(s.$t(`examples_${p.value}`)),1)]),default:te(()=>[e("span",Oe,[V(ue)])]),_:1})])):U("",!0),p.value===l(m).INTERFACE?f((u(),r("select",{key:1,class:"form-select mt-2","onUpdate:modelValue":d[2]||(d[2]=t=>ee(c)?c.value=t:null)},[e("option",Ue,o(s.$t("all_local_networks")),1),(u(!0),r(y,null,k((W=h.networks)==null?void 0:W.filter(t=>t.type!=="wan"),t=>(u(),r("option",{value:t.ifName},o(t.name),9,Le))),256))],512)),[[E,l(c)]]):U("",!0),l(v)?(u(),r("div",Je,o(l(v)?s.$t(l(v)):""),1)):U("",!0)])]),e("div",je,[e("label",Be,o(l(C)("route_via")),1),e("div",Pe,[f(e("select",{class:"form-select","onUpdate:modelValue":d[3]||(d[3]=t=>i.if_name=t)},[(u(!0),r(y,null,k((X=h.networks)==null?void 0:X.filter(t=>["wan","vpn"].includes(t.type)),t=>(u(),r("option",{key:t.ifName,value:t.ifName},o(t.name),9,Ke))),128))],512),[[E,i.if_name]])])]),e("div",Qe,[e("label",Ze,o(l(C)("apply_to")),1),e("div",ze,[f(e("select",{class:"form-select","onUpdate:modelValue":d[4]||(d[4]=t=>i.apply_to=t)},[e("option",Ge,o(s.$t("all_devices")),1),(u(!0),r(y,null,k((Y=h.networks)==null?void 0:Y.filter(t=>!["wan","vpn"].includes(t.type)),t=>(u(),r("option",{key:t.ifName,value:"iface:"+t.ifName},o(t.name),9,He))),128)),(u(!0),r(y,null,k(h.devices,t=>(u(),r("option",{value:"mac:"+t.mac},o(t.name),9,We))),256))],512),[[E,i.apply_to]])])]),e("div",Xe,[e("label",Ye,o(l(C)("notes")),1),e("div",xe,[f(e("textarea",{class:"form-control","onUpdate:modelValue":d[5]||(d[5]=t=>i.notes=t),rows:"3"},null,512),[[x,i.notes]])])])]),e("div",et,[e("md-outlined-button",{value:"cancel",onClick:d[6]||(d[6]=(...t)=>l(F)&&l(F)(...t))},o(s.$t("cancel")),1),e("md-filled-button",{value:"save",disabled:l(q)||l(n),onClick:d[7]||(d[7]=(...t)=>l(N)&&l(N)(...t)),autofocus:""},o(s.$t("save")),9,tt)])])}}}),at={class:"page-container"},ot={class:"main"},nt={class:"v-toolbar"},st={class:"table-responsive"},lt={class:"table"},it=e("th",null,"ID",-1),dt={class:"actions two"},ut={class:"form-check"},rt=["disabled","onChange","checked"],ct={class:"nowrap"},pt={class:"nowrap"},_t={class:"actions two"},mt=["onClick"],vt=["onClick"],bt=ie({__name:"RoutesView",setup(h){const _=A([]),b=A([]),i=A([]),{t:p}=de();fe({handle:(n,T)=>{T?ge(p(T),"error"):(_.value=n.configs.filter(c=>c.group==="route").map(c=>{const g=JSON.parse(c.value),v=new Te;v.parse(g.apply_to);const a=new w;return a.parse(g.target),{id:c.id,createdAt:c.createdAt,updatedAt:c.updatedAt,data:g,applyTo:v,target:a}}),b.value=[...n.devices],i.value=[...n.networks])},document:L`
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
    ${$e}
    ${I}
    ${he}
  `});function D(n){J(ye,{id:n.id,name:n.id,gql:L`
      mutation DeleteConfig($id: ID!) {
        deleteConfig(id: $id)
      }
    `,appApi:!1,typeName:"Config"})}function C(n){J(le,{data:n,devices:b,networks:i})}function R(){J(le,{data:null,devices:b,networks:i})}const{mutate:q,loading:M}=j({document:L`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${I}
  `});function O(n){q({id:n.id,input:{group:"route",value:JSON.stringify(n.data)}})}return(n,T)=>{const c=we,g=ke,v=be("tooltip");return u(),r("div",at,[e("div",ot,[e("div",nt,[V(c,{current:()=>n.$t("page_title.routes")},null,8,["current"]),e("button",{type:"button",class:"btn right-actions",onClick:R},o(n.$t("create")),1)]),e("div",st,[e("table",lt,[e("thead",null,[e("tr",null,[it,e("th",null,o(n.$t("apply_to")),1),e("th",null,o(n.$t("description")),1),e("th",null,o(n.$t("notes")),1),e("th",null,o(n.$t("enabled")),1),e("th",null,o(n.$t("created_at")),1),e("th",null,o(n.$t("updated_at")),1),e("th",dt,o(n.$t("actions")),1)])]),e("tbody",null,[(u(!0),r(y,null,k(_.value,a=>{var N;return u(),r("tr",{key:a.id},[e("td",null,[V(g,{id:a.id,raw:a.data},null,8,["id","raw"])]),e("td",null,o(a.applyTo.getText(n.$t,b.value,i.value)),1),e("td",null,o(n.$t("route_description",{if_name:((N=i.value.find($=>$.ifName==a.data.if_name))==null?void 0:N.name)??a.data.if_name,target:a.target.getText(n.$t,i.value)})),1),e("td",null,o(a.notes),1),e("td",null,[e("div",ut,[e("md-checkbox",{"touch-target":"wrapper",disabled:l(M),onChange:$=>O(a),checked:a.data.is_enabled},null,40,rt)])]),e("td",ct,[f((u(),r("span",null,[oe(o(l(ne)(a.createdAt)),1)])),[[v,l(ae)(a.createdAt)]])]),e("td",pt,[f((u(),r("span",null,[oe(o(l(ne)(a.updatedAt)),1)])),[[v,l(ae)(a.updatedAt)]])]),e("td",_t,[e("a",{href:"#",class:"v-link",onClick:se($=>C(a),["prevent"])},o(n.$t("edit")),9,mt),e("a",{href:"#",class:"v-link",onClick:se($=>D(a),["prevent"])},o(n.$t("delete")),9,vt)])])}),128))])])])])])}}});export{bt as default};

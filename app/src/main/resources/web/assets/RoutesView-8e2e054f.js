import{d as ie,K as ce,r as A,u as de,i as j,aq as S,c3 as F,c4 as pe,an as _e,w as ve,ao as I,o as u,c as r,a as e,t as o,j as l,m as f,c6 as E,F as y,J as k,v as x,n as ee,p as V,H as te,k as U,at as me,g as fe,x as ge,ab as J,c7 as $e,c8 as he,R as be,a2 as L,Y as ae,h as oe,Z as ne,l as se,ac as ye,a5 as ke}from"./index-9c78c93b.js";import{_ as we}from"./Breadcrumb-56c0fca0.js";import{T as v,a as w,_ as Ce,A as Te}from"./question-mark-rounded-e7c8f902.js";import{u as Ne,a as Fe}from"./vee-validate.esm-2d465d6a.js";const Ie={slot:"headline"},Ee={slot:"content"},Ae={class:"row mb-3"},Ve={class:"col-md-3 col-form-label"},De={class:"col-md-9"},Re=["value"],Me={key:0,class:"input-group"},qe=["placeholder"],Oe={class:"inner"},Se={class:"help-block"},Ue={value:""},Je=["value"],Le={key:2,class:"invalid-feedback"},je={class:"row mb-3"},Be={class:"col-md-3 col-form-label"},Pe={class:"col-md-9"},He=["value"],Ke={class:"row mb-3"},Qe={class:"col-md-3 col-form-label"},Ye={class:"col-md-9"},Ze={value:"all"},ze=["value"],Ge=["value"],We={class:"row mb-3"},Xe={class:"col-md-3 col-form-label"},xe={class:"col-md-9"},et={slot:"actions"},tt=["disabled"],le=ie({__name:"EditRouteModal",props:{data:{type:Object},devices:{type:Array},networks:{type:Array}},setup(h){var $,B,P,H,K,Q,Y,Z,z;const _=h,{handleSubmit:b}=Ne(),i=ce({if_name:"",apply_to:"all",notes:"",target:"",is_enabled:!0}),p=A(v.INTERNET),D=Object.values(v).filter(s=>[v.IP,v.NET,v.REMOTE_PORT,v.INTERNET].includes(s)),{t:C}=de(),{mutate:R,loading:M,onDone:q}=j({document:S`
    mutation createConfig($input: ConfigInput!) {
      createConfig(input: $input) {
        ...ConfigFragment
      }
    }
    ${F}
  `,options:{update:(s,d)=>{pe(s,d.data.createConfig,S`
          query {
            configs {
              ...ConfigFragment
            }
          }
          ${F}
        `)}}}),{mutate:O,loading:n,onDone:T}=j({document:S`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${F}
  `}),{value:c,resetField:g,errorMessage:m}=Fe("inputValue",_e().test("required",s=>"valid.required",s=>!w.hasInput(p.value)||!!s).test("target-value",s=>"invalid_value",s=>w.isValid(p.value,s??""))),a=($=_.data)==null?void 0:$.data;p.value=((P=(B=_.data)==null?void 0:B.target)==null?void 0:P.type)??v.INTERNET,c.value=((K=(H=_.data)==null?void 0:H.target)==null?void 0:K.value)??"",i.apply_to=((Y=(Q=_.data)==null?void 0:Q.applyTo)==null?void 0:Y.toValue())??"all",i.if_name=(a==null?void 0:a.if_name)??((z=(Z=_.networks)==null?void 0:Z[0])==null?void 0:z.ifName)??"",i.notes=(a==null?void 0:a.notes)??"",i.is_enabled=(a==null?void 0:a.is_enabled)??!0,a||g(),ve(p,(s,d)=>{(s===v.INTERFACE||d===v.INTERFACE)&&(c.value="")});const N=b(()=>{const s=new w;s.type=p.value,s.value=c.value??"",i.target=s.toValue(),_.data?O({id:_.data.id,input:{group:"route",value:JSON.stringify(i)}}):R({input:{group:"route",value:JSON.stringify(i)}})});return q(()=>{I()}),T(()=>{I()}),(s,d)=>{var G,W,X;const ue=Ce,re=me;return u(),r("md-dialog",null,[e("div",Ie,o(l(a)?s.$t("edit"):s.$t("create")),1),e("div",Ee,[e("div",Ae,[e("label",Ve,o(s.$t("traffic_to")),1),e("div",De,[f(e("select",{class:"form-select","onUpdate:modelValue":d[0]||(d[0]=t=>p.value=t)},[(u(!0),r(y,null,k(l(D),t=>(u(),r("option",{value:t},o(s.$t(`target_type.${t}`)),9,Re))),256))],512),[[E,p.value]]),l(w).hasInput(p.value)?(u(),r("div",Me,[f(e("input",{type:"text",class:"form-control","onUpdate:modelValue":d[1]||(d[1]=t=>ee(c)?c.value=t:null),placeholder:s.$t("for_example")+" "+l(w).hint(p.value)},null,8,qe),[[x,l(c)]]),V(re,{class:"input-group-text"},{content:te(()=>[e("pre",Se,o(s.$t(`examples_${p.value}`)),1)]),default:te(()=>[e("span",Oe,[V(ue)])]),_:1})])):U("",!0),p.value===l(v).INTERFACE?f((u(),r("select",{key:1,class:"form-select","onUpdate:modelValue":d[2]||(d[2]=t=>ee(c)?c.value=t:null)},[e("option",Ue,o(s.$t("all_local_networks")),1),(u(!0),r(y,null,k((G=h.networks)==null?void 0:G.filter(t=>t.type!=="wan"),t=>(u(),r("option",{value:t.ifName},o(t.name),9,Je))),256))],512)),[[E,l(c)]]):U("",!0),l(m)?(u(),r("div",Le,o(l(m)?s.$t(l(m)):""),1)):U("",!0)])]),e("div",je,[e("label",Be,o(l(C)("route_via")),1),e("div",Pe,[f(e("select",{class:"form-select","onUpdate:modelValue":d[3]||(d[3]=t=>i.if_name=t)},[(u(!0),r(y,null,k((W=h.networks)==null?void 0:W.filter(t=>["wan","vpn"].includes(t.type)),t=>(u(),r("option",{key:t.ifName,value:t.ifName},o(t.name),9,He))),128))],512),[[E,i.if_name]])])]),e("div",Ke,[e("label",Qe,o(l(C)("apply_to")),1),e("div",Ye,[f(e("select",{class:"form-select","onUpdate:modelValue":d[4]||(d[4]=t=>i.apply_to=t)},[e("option",Ze,o(s.$t("all_devices")),1),(u(!0),r(y,null,k((X=h.networks)==null?void 0:X.filter(t=>!["wan","vpn"].includes(t.type)),t=>(u(),r("option",{key:t.ifName,value:"iface:"+t.ifName},o(t.name),9,ze))),128)),(u(!0),r(y,null,k(h.devices,t=>(u(),r("option",{value:"mac:"+t.mac},o(t.name),9,Ge))),256))],512),[[E,i.apply_to]])])]),e("div",We,[e("label",Xe,o(l(C)("notes")),1),e("div",xe,[f(e("textarea",{class:"form-control","onUpdate:modelValue":d[5]||(d[5]=t=>i.notes=t),rows:"3"},null,512),[[x,i.notes]])])])]),e("div",et,[e("md-outlined-button",{value:"cancel",onClick:d[6]||(d[6]=(...t)=>l(I)&&l(I)(...t))},o(s.$t("cancel")),1),e("md-filled-button",{value:"save",disabled:l(M)||l(n),onClick:d[7]||(d[7]=(...t)=>l(N)&&l(N)(...t)),autofocus:""},o(s.$t("save")),9,tt)])])}}}),at={class:"page-container"},ot={class:"main"},nt={class:"v-toolbar"},st={class:"table-responsive"},lt={class:"table"},it=e("th",null,"ID",-1),dt={class:"actions two"},ut={class:"form-check"},rt=["disabled","onChange","checked"],ct={class:"nowrap"},pt={class:"nowrap"},_t={class:"actions two"},vt=["onClick"],mt=["onClick"],bt=ie({__name:"RoutesView",setup(h){const _=A([]),b=A([]),i=A([]),{t:p}=de();fe({handle:(n,T)=>{T?ge(p(T),"error"):(_.value=n.configs.filter(c=>c.group==="route").map(c=>{const g=JSON.parse(c.value),m=new Te;m.parse(g.apply_to);const a=new w;return a.parse(g.target),{id:c.id,createdAt:c.createdAt,updatedAt:c.updatedAt,data:g,applyTo:m,target:a}}),b.value=[...n.devices],i.value=[...n.networks])},document:J`
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
    ${F}
    ${he}
  `});function D(n){L(ye,{id:n.id,name:n.id,gql:J`
      mutation DeleteConfig($id: ID!) {
        deleteConfig(id: $id)
      }
    `,appApi:!1,typeName:"Config"})}function C(n){L(le,{data:n,devices:b,networks:i})}function R(){L(le,{data:null,devices:b,networks:i})}const{mutate:M,loading:q}=j({document:J`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${F}
  `});function O(n){M({id:n.id,input:{group:"route",value:JSON.stringify(n.data)}})}return(n,T)=>{const c=we,g=ke,m=be("tooltip");return u(),r("div",at,[e("div",ot,[e("div",nt,[V(c,{current:()=>n.$t("page_title.routes")},null,8,["current"]),e("button",{type:"button",class:"btn right-actions",onClick:R},o(n.$t("create")),1)]),e("div",st,[e("table",lt,[e("thead",null,[e("tr",null,[it,e("th",null,o(n.$t("apply_to")),1),e("th",null,o(n.$t("description")),1),e("th",null,o(n.$t("notes")),1),e("th",null,o(n.$t("enabled")),1),e("th",null,o(n.$t("created_at")),1),e("th",null,o(n.$t("updated_at")),1),e("th",dt,o(n.$t("actions")),1)])]),e("tbody",null,[(u(!0),r(y,null,k(_.value,a=>{var N;return u(),r("tr",{key:a.id},[e("td",null,[V(g,{id:a.id,raw:a.data},null,8,["id","raw"])]),e("td",null,o(a.applyTo.getText(n.$t,b.value,i.value)),1),e("td",null,o(n.$t("route_description",{if_name:((N=i.value.find($=>$.ifName==a.data.if_name))==null?void 0:N.name)??a.data.if_name,target:a.target.getText(n.$t,i.value)})),1),e("td",null,o(a.notes),1),e("td",null,[e("div",ut,[e("md-checkbox",{"touch-target":"wrapper",disabled:l(q),onChange:$=>O(a),checked:a.data.is_enabled},null,40,rt)])]),e("td",ct,[f((u(),r("span",null,[oe(o(l(ne)(a.createdAt)),1)])),[[m,l(ae)(a.createdAt)]])]),e("td",pt,[f((u(),r("span",null,[oe(o(l(ne)(a.updatedAt)),1)])),[[m,l(ae)(a.updatedAt)]])]),e("td",_t,[e("a",{href:"#",class:"v-link",onClick:se($=>C(a),["prevent"])},o(n.$t("edit")),9,vt),e("a",{href:"#",class:"v-link",onClick:se($=>D(a),["prevent"])},o(n.$t("delete")),9,mt)])])}),128))])])])])])}}});export{bt as default};

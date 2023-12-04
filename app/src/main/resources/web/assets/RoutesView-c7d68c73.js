import{d as ie,B as ce,r as E,u as de,a2 as J,ao as S,bZ as F,b_ as pe,ak as _e,G as me,al as I,o as u,e as r,f as e,t as o,h as l,L as f,c0 as A,F as y,A as k,R as x,am as ee,x as V,y as te,j as U,ar as ve,i as fe,k as ge,a8 as L,c1 as $e,c2 as he,K as be,Y as B,T as ae,g as oe,U as ne,w as se,a9 as ye,a0 as ke}from"./index-e23e99bf.js";import{_ as we}from"./Breadcrumb-ba8f6aaf.js";import{T as m,a as w,_ as Ce,A as Te}from"./question-mark-rounded-73460fa5.js";import{u as Ne,a as Fe}from"./vee-validate.esm-36997df0.js";const Ie={slot:"headline"},Ae={slot:"content"},Ee={class:"row mb-3"},Ve={class:"col-md-3 col-form-label"},De={class:"col-md-9"},Re=["value"],Me={key:0,class:"input-group mt-2"},qe=["placeholder"],Oe={class:"inner"},Se={class:"help-block"},Ue={value:""},Le=["value"],Be={key:2,class:"invalid-feedback"},Je={class:"row mb-3"},je={class:"col-md-3 col-form-label"},Pe={class:"col-md-9"},Ge=["value"],Ke={class:"row mb-3"},Qe={class:"col-md-3 col-form-label"},Ye={class:"col-md-9"},Ze={value:"all"},ze=["value"],He=["value"],We={class:"row mb-3"},Xe={class:"col-md-3 col-form-label"},xe={class:"col-md-9"},et={slot:"actions"},tt=["disabled"],le=ie({__name:"EditRouteModal",props:{data:{type:Object},devices:{type:Array},networks:{type:Array}},setup(h){var $,j,P,G,K,Q,Y,Z,z;const _=h,{handleSubmit:b}=Ne(),i=ce({if_name:"",apply_to:"all",notes:"",target:"",is_enabled:!0}),p=E(m.INTERNET),D=Object.values(m).filter(s=>[m.IP,m.NET,m.REMOTE_PORT,m.INTERNET].includes(s)),{t:C}=de(),{mutate:R,loading:M,onDone:q}=J({document:S`
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
        `)}}}),{mutate:O,loading:n,onDone:T}=J({document:S`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${F}
  `}),{value:c,resetField:g,errorMessage:v}=Fe("inputValue",_e().test("required",s=>"valid.required",s=>!w.hasInput(p.value)||!!s).test("target-value",s=>"invalid_value",s=>w.isValid(p.value,s??""))),a=($=_.data)==null?void 0:$.data;p.value=((P=(j=_.data)==null?void 0:j.target)==null?void 0:P.type)??m.INTERNET,c.value=((K=(G=_.data)==null?void 0:G.target)==null?void 0:K.value)??"",i.apply_to=((Y=(Q=_.data)==null?void 0:Q.applyTo)==null?void 0:Y.toValue())??"all",i.if_name=(a==null?void 0:a.if_name)??((z=(Z=_.networks)==null?void 0:Z[0])==null?void 0:z.ifName)??"",i.notes=(a==null?void 0:a.notes)??"",i.is_enabled=(a==null?void 0:a.is_enabled)??!0,a||g(),me(p,(s,d)=>{(s===m.INTERFACE||d===m.INTERFACE)&&(c.value="")});const N=b(()=>{const s=new w;s.type=p.value,s.value=c.value??"",i.target=s.toValue(),_.data?O({id:_.data.id,input:{group:"route",value:JSON.stringify(i)}}):R({input:{group:"route",value:JSON.stringify(i)}})});return q(()=>{I()}),T(()=>{I()}),(s,d)=>{var H,W,X;const ue=Ce,re=ve;return u(),r("md-dialog",null,[e("div",Ie,o(l(a)?s.$t("edit"):s.$t("create")),1),e("div",Ae,[e("div",Ee,[e("label",Ve,o(s.$t("traffic_to")),1),e("div",De,[f(e("select",{class:"form-select","onUpdate:modelValue":d[0]||(d[0]=t=>p.value=t)},[(u(!0),r(y,null,k(l(D),t=>(u(),r("option",{value:t},o(s.$t(`target_type.${t}`)),9,Re))),256))],512),[[A,p.value]]),l(w).hasInput(p.value)?(u(),r("div",Me,[f(e("input",{type:"text",class:"form-control","onUpdate:modelValue":d[1]||(d[1]=t=>ee(c)?c.value=t:null),placeholder:s.$t("for_example")+" "+l(w).hint(p.value)},null,8,qe),[[x,l(c)]]),V(re,{class:"input-group-text"},{content:te(()=>[e("pre",Se,o(s.$t(`examples_${p.value}`)),1)]),default:te(()=>[e("span",Oe,[V(ue)])]),_:1})])):U("",!0),p.value===l(m).INTERFACE?f((u(),r("select",{key:1,class:"form-select mt-2","onUpdate:modelValue":d[2]||(d[2]=t=>ee(c)?c.value=t:null)},[e("option",Ue,o(s.$t("all_local_networks")),1),(u(!0),r(y,null,k((H=h.networks)==null?void 0:H.filter(t=>t.type!=="wan"),t=>(u(),r("option",{value:t.ifName},o(t.name),9,Le))),256))],512)),[[A,l(c)]]):U("",!0),l(v)?(u(),r("div",Be,o(l(v)?s.$t(l(v)):""),1)):U("",!0)])]),e("div",Je,[e("label",je,o(l(C)("route_via")),1),e("div",Pe,[f(e("select",{class:"form-select","onUpdate:modelValue":d[3]||(d[3]=t=>i.if_name=t)},[(u(!0),r(y,null,k((W=h.networks)==null?void 0:W.filter(t=>["wan","vpn"].includes(t.type)),t=>(u(),r("option",{key:t.ifName,value:t.ifName},o(t.name),9,Ge))),128))],512),[[A,i.if_name]])])]),e("div",Ke,[e("label",Qe,o(l(C)("apply_to")),1),e("div",Ye,[f(e("select",{class:"form-select","onUpdate:modelValue":d[4]||(d[4]=t=>i.apply_to=t)},[e("option",Ze,o(s.$t("all_devices")),1),(u(!0),r(y,null,k((X=h.networks)==null?void 0:X.filter(t=>!["wan","vpn"].includes(t.type)),t=>(u(),r("option",{key:t.ifName,value:"iface:"+t.ifName},o(t.name),9,ze))),128)),(u(!0),r(y,null,k(h.devices,t=>(u(),r("option",{value:"mac:"+t.mac},o(t.name),9,He))),256))],512),[[A,i.apply_to]])])]),e("div",We,[e("label",Xe,o(l(C)("notes")),1),e("div",xe,[f(e("textarea",{class:"form-control","onUpdate:modelValue":d[5]||(d[5]=t=>i.notes=t),rows:"3"},null,512),[[x,i.notes]])])])]),e("div",et,[e("md-outlined-button",{value:"cancel",onClick:d[6]||(d[6]=(...t)=>l(I)&&l(I)(...t))},o(s.$t("cancel")),1),e("md-filled-button",{value:"save",disabled:l(M)||l(n),onClick:d[7]||(d[7]=(...t)=>l(N)&&l(N)(...t)),autofocus:""},o(s.$t("save")),9,tt)])])}}}),at={class:"page-container"},ot={class:"main"},nt={class:"v-toolbar"},st={class:"table-responsive"},lt={class:"table"},it=e("th",null,"ID",-1),dt={class:"actions two"},ut={class:"form-check"},rt=["disabled","onChange","checked"],ct={class:"nowrap"},pt={class:"nowrap"},_t={class:"actions two"},mt=["onClick"],vt=["onClick"],bt=ie({__name:"RoutesView",setup(h){const _=E([]),b=E([]),i=E([]),{t:p}=de();fe({handle:(n,T)=>{T?ge(p(T),"error"):(_.value=n.configs.filter(c=>c.group==="route").map(c=>{const g=JSON.parse(c.value),v=new Te;v.parse(g.apply_to);const a=new w;return a.parse(g.target),{id:c.id,createdAt:c.createdAt,updatedAt:c.updatedAt,data:g,applyTo:v,target:a}}),b.value=[...n.devices],i.value=[...n.networks])},document:L`
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
  `});function D(n){B(ye,{id:n.id,name:n.id,gql:L`
      mutation DeleteConfig($id: ID!) {
        deleteConfig(id: $id)
      }
    `,appApi:!1,typeName:"Config"})}function C(n){B(le,{data:n,devices:b,networks:i})}function R(){B(le,{data:null,devices:b,networks:i})}const{mutate:M,loading:q}=J({document:L`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${F}
  `});function O(n){M({id:n.id,input:{group:"route",value:JSON.stringify(n.data)}})}return(n,T)=>{const c=we,g=ke,v=be("tooltip");return u(),r("div",at,[e("div",ot,[e("div",nt,[V(c,{current:()=>n.$t("page_title.routes")},null,8,["current"]),e("button",{type:"button",class:"btn right-actions",onClick:R},o(n.$t("create")),1)]),e("div",st,[e("table",lt,[e("thead",null,[e("tr",null,[it,e("th",null,o(n.$t("apply_to")),1),e("th",null,o(n.$t("description")),1),e("th",null,o(n.$t("notes")),1),e("th",null,o(n.$t("enabled")),1),e("th",null,o(n.$t("created_at")),1),e("th",null,o(n.$t("updated_at")),1),e("th",dt,o(n.$t("actions")),1)])]),e("tbody",null,[(u(!0),r(y,null,k(_.value,a=>{var N;return u(),r("tr",{key:a.id},[e("td",null,[V(g,{id:a.id,raw:a.data},null,8,["id","raw"])]),e("td",null,o(a.applyTo.getText(n.$t,b.value,i.value)),1),e("td",null,o(n.$t("route_description",{if_name:((N=i.value.find($=>$.ifName==a.data.if_name))==null?void 0:N.name)??a.data.if_name,target:a.target.getText(n.$t,i.value)})),1),e("td",null,o(a.notes),1),e("td",null,[e("div",ut,[e("md-checkbox",{"touch-target":"wrapper",disabled:l(q),onChange:$=>O(a),checked:a.data.is_enabled},null,40,rt)])]),e("td",ct,[f((u(),r("span",null,[oe(o(l(ne)(a.createdAt)),1)])),[[v,l(ae)(a.createdAt)]])]),e("td",pt,[f((u(),r("span",null,[oe(o(l(ne)(a.updatedAt)),1)])),[[v,l(ae)(a.updatedAt)]])]),e("td",_t,[e("a",{href:"#",class:"v-link",onClick:se($=>C(a),["prevent"])},o(n.$t("edit")),9,mt),e("a",{href:"#",class:"v-link",onClick:se($=>D(a),["prevent"])},o(n.$t("delete")),9,vt)])])}),128))])])])])])}}});export{bt as default};
